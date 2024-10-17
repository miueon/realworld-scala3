package realworld.service

import cats.Functor
import cats.data.EitherT
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all.*
import dev.profunktor.redis4cats.RedisCommands
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Codec, Decoder, DecodingFailure, Encoder}
import org.typelevel.log4cats.Logger
import realworld.auth.{Crypto, JWT}
import realworld.codec.given
import realworld.config.types.TokenExpiration
import realworld.db.DoobieTx
import realworld.domain.user.{DBUser, UserError, UserId}
import realworld.domain.{ID, WithId}
import realworld.effects.GenUUID
import realworld.repo.UserRepo
import realworld.spec.{
  AuthHeader,
  CredentialsError,
  LoginUserInputData,
  RegisterUserData,
  Token,
  UnauthorizedError,
  UpdateUserData,
  User
}
import realworld.types.{Email, Username}

trait Auth[F[_]: Functor]:
  def login(user: LoginUserInputData): F[User]
  def register(user: RegisterUserData): F[User]
  def access(header: AuthHeader): F[UserSession]
  def authUserId(header: AuthHeader): F[UserId] =
    access(header).map(_.id)
  def update(userSession: UserSession, updateData: UpdateUserData): F[UserSession]

case class UserSession(id: UserId, user: User) derives Codec.AsObject

object Auth:
  def make[F[_]: MonadCancelThrow: GenUUID: DoobieTx: Logger](
      tokenExpiration: TokenExpiration,
      jwt: JWT[F],
      userRepo: UserRepo[F],
      redis: RedisCommands[F, String, String],
      crypto: Crypto
  ): Auth[F] =
    new:
      private val TokenExpiration = tokenExpiration.value
      def login(user: LoginUserInputData): F[User] =
        userRepo
          .findByEmail(user.email)
          .flatMap:
            case None => UserError.UserNotFound().raiseError[F, User]
            case Some(WithId(_, dbUser))
                if !crypto.verifyPassword(user.password, dbUser.password) =>
              UserError.UserPasswordNotMatched().raiseError[F, User]
            case Some(WithId(uid, dbUser)) =>
              redis
                .get(user.email)
                .flatMap:
                  case Some(t) =>
                    dbUser.toUser(Token(t).some).pure[F]
                  case None =>
                    jwt.create
                      .flatMap: t =>
                        dbUser.toUser(t.some).pure[F]
                      .flatTap { u =>
                        val token = u.token.get.value
                        redis.setEx(
                          token,
                          UserSession(uid, u).asJson.noSpaces,
                          TokenExpiration
                        ) *>
                          redis.setEx(
                            user.email,
                            token,
                            TokenExpiration
                          )
                      }
      end login

      def register(user: RegisterUserData): F[User] =
        userRepo
          .findByEmail(user.email)
          .flatMap:
            case Some(_) => UserError.EmailAlreadyExists().raiseError[F, User]
            case None =>
              val encryptedPassword = crypto.encrypt(user.password)
              for
                uid <- ID.make[F, UserId]
                _ <- userRepo.tx.use { tx =>
                  tx.create(uid, user, encryptedPassword) *> Logger[F].info("")
                }
                jwt <- jwt.create
                _ <- redis.setEx(
                  jwt.value,
                  UserSession(
                    uid,
                    User(user.email, user.username)
                  ).asJson.noSpaces,
                  TokenExpiration
                )
                _ <- redis.setEx(user.email, jwt.value, TokenExpiration)
              yield User(
                email = user.email,
                username = user.username,
                token = jwt.some
              )
              end for
      end register

      def access(header: AuthHeader): F[UserSession] =
        val result = for
          token <- EitherT.fromOption(
            extractTokenValue(header),
            CredentialsError(s"Auth header incorrect=$header")
          )
          u <- EitherT.fromOptionF(
            redis.get(token),
            UnauthorizedError(s"Token expired or not found, toke=$token".some)
          )
          session <- EitherT.fromOption(
            decode[UserSession](u).toOption,
            UnauthorizedError("Token decode error".some)
          )
        yield session
        result.value.flatMap:
          case Right(s) => s.pure[F]
          case Left(e)  => e.raiseError[F, UserSession]
      end access

      def update(userSession: UserSession, updateData: UpdateUserData): F[UserSession] =
        // val emailCleanOpt =
        //   updateData.email.map(e => e.value.toLowerCase.trim())
        // val usernameCleanOpt =
        //   updateData.username.map(un => un.value.trim())

        val hasedPsw = updateData.password.map(crypto.encrypt(_))
        val uid      = userSession.id
        val token    = userSession.user.token.get.value
        for
          _ <- updateData.email.traverse(emailNotUsed(_, uid))
          _ <- updateData.username.traverse(usernameNotUsed(_, uid))
          row <- userRepo.tx.use {
            _.update(uid, updateData, hasedPsw).flatMap {
              case Some(u) => u.pure[F]
              case None    => UserError.UserNotFound().raiseError
            }
          }
          session = userSession.copy(user = row.entity.toUser(userSession.user.token))
          _ <- redis.setEx(token, session.asJson.noSpaces, TokenExpiration)
          _ <- redis.setEx(row.entity.email, token, TokenExpiration)
        yield session
      end update

      val tokenPattern = "^Token (.+)".r
      def extractTokenValue(s: AuthHeader): Option[String] =
        s.value match
          case tokenPattern(value) => value.some
          case _                   => None

      def emailNotUsed(email: Email, userId: UserId): F[Unit] =
        userRepo
          .findByEmail(email)
          .flatMap(notTakenByOthers(_, userId, UserError.EmailAlreadyExists()))

      def notTakenByOthers(
          user: Option[WithId[UserId, DBUser]],
          userId: UserId,
          error: UserError
      ): F[Unit] =
        user match
          case Some(WithId(id, _)) if id =!= userId => error.raiseError[F, Unit]
          case _                                    => ().pure[F]

      def usernameNotUsed(username: Username, userId: UserId): F[Unit] =
        userRepo
          .findByUsername(username)
          .flatMap(notTakenByOthers(_, userId, UserError.UsernameAlreadyExists()))
end Auth
