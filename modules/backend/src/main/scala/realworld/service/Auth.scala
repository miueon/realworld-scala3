package realworld.service

import cats.Functor
import cats.Monad
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all.*

import dev.profunktor.redis4cats.RedisCommands
import io.circe.Codec
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.Json
import io.circe.parser.decode
import io.circe.syntax.*
import org.typelevel.log4cats.Logger
import pdi.jwt.JwtClaim
import realworld.auth.Crypto
import realworld.auth.JWT
import realworld.config.types.TokenExpiration
import realworld.domain.ID
import realworld.domain.given
import realworld.domain.user.DBUser
import realworld.domain.user.UserId
import realworld.domain.user.Users.username
import realworld.effects.GenUUID
import realworld.spec.AuthHeader
import realworld.spec.ForbiddenError
import realworld.spec.LoginUserInputData
import realworld.spec.NotFoundError
import realworld.spec.RegisterUserData
import realworld.spec.Token
import realworld.spec.UnauthorizedError
import realworld.spec.User
import smithy4s.Document
import smithy4s.schema.Schema
import realworld.domain.user.UserError
import realworld.domain.WithId
import realworld.spec.UpdateUserData
import realworld.spec.Email
import realworld.spec.Username
import realworld.domain.user.EncryptedPassword
import doobie.util.transactor.Transactor
import realworld.db.transaction
import realworld.db.transactK
import realworld.db.DoobieTx
import realworld.repo.UserRepo
import realworld.spec.CredentialsError
import cats.data.OptionT
import cats.data.EitherT

trait Auth[F[_]: Functor]:
  def login(user: LoginUserInputData): F[User]
  def register(user: RegisterUserData): F[User]
  def access(header: AuthHeader): F[UserSession]
  def authUserId(header: AuthHeader): F[UserId] =
    access(header).map(_.id)
  def update(uid: UserId, updateData: UpdateUserData): F[DBUser]

case class UserSession(id: UserId, user: User) derives Codec.AsObject

object Auth:
  def make[F[_]: MonadCancelThrow: GenUUID: DoobieTx](
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
                .get(user.email.value)
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
                            user.email.value,
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
                _   <- userRepo.tx.use { _.create(uid, user, encryptedPassword) }
                jwt <- jwt.create
                _ <- redis.setEx(
                  jwt.value,
                  UserSession(
                    uid,
                    User(user.email, user.username)
                  ).asJson.noSpaces,
                  TokenExpiration
                )
                _ <- redis.setEx(user.email.value, jwt.value, TokenExpiration)
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

      def update(uid: UserId, updateData: UpdateUserData): F[DBUser] =
        val emailCleanOpt =
          updateData.email.map(e => e.value.toLowerCase.trim())
        val usernameCleanOpt =
          updateData.username.map(un => un.value.trim())

        val hasedPsw = updateData.password.map(crypto.encrypt(_))
        val userOpt = for
          _   <- updateData.email.traverse(emailNotUsed(_, uid))
          _   <- updateData.username.traverse(usernameNotUsed(_, uid))
          row <- userRepo.tx.use { _.update(uid, updateData, hasedPsw) }
        yield row
        userOpt.flatMap:
          case Some(WithId(_, user)) => user.pure[F]
          case None                  => UserError.UserNotFound().raiseError[F, DBUser]
      end update

      val tokenPattern = "^Token (.+)".r
      def extractTokenValue(s: AuthHeader): Option[String] =
        s.value match
          case tokenPattern(value) => value.some
          case _                   => None

      def emailNotUsed(email: Email, userId: UserId): F[Unit] =
        userRepo
          .findByEmail(email)
          .map(notTakenByOthers(_, userId, UserError.EmailAlreadyExists()))

      def notTakenByOthers(
          user: Option[WithId[UserId, DBUser]],
          userId: UserId,
          error: UserError
      ): Unit =
        user match
          case Some(WithId(id, _)) if id =!= userId => error.raiseError[F, Unit]
          case _                                    => ().pure[F]

      def usernameNotUsed(username: Username, userId: UserId): F[Unit] =
        userRepo
          .findByUsername(username)
          .map(notTakenByOthers(_, userId, UserError.UsernameAlreadyExists()))
end Auth
