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
import realworld.domain.users.DBUser
import realworld.domain.users.UserId
import realworld.domain.users.Users.username
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
import realworld.domain.users.UserError
import realworld.domain.WithId
import realworld.spec.UpdateUserData
import realworld.spec.Email
import realworld.spec.Username
import realworld.domain.users.EncryptedPassword
import doobie.util.transactor.Transactor
import realworld.db.transaction
import realworld.db.transactK
import realworld.db.DoobieTx
import realworld.repo.UserRepo

trait Auth[F[_]]:
  def login(user: LoginUserInputData): F[User]
  def register(user: RegisterUserData): F[User]
  def access(header: AuthHeader): F[UserSession]
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
            case Some(WithId(_, dbUser)) =>
              redis
                .get(user.email.value)
                .flatMap:
                  case Some(t) =>
                    dbUser.toUser(Token(t).some).pure[F]
                  case None =>
                    jwt.create.flatMap: t =>
                      redis.setEx(
                        t.value,
                        dbUser.asJson.noSpaces,
                        TokenExpiration
                      )
                      redis.setEx(
                        user.email.value,
                        t.value,
                        TokenExpiration
                      )
                      dbUser.toUser(t.some).pure[F]
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
        if header.value.startsWith("Bearer ") then
          val token = header.value.drop("Bearer ".length)
          redis
            .get(token)
            .flatMap {
              _.flatMap { u =>
                decode[UserSession](u).toOption
              }.fold(UnauthorizedError().raiseError[F, UserSession])(
                _.pure[F]
              )
            }
        else UnauthorizedError().raiseError[F, UserSession]

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

// object UserAuth:
//   def make[F[_]: Functor](
//       redis: RedisCommands[F, String, String]
//   ): UserAuth[F] =
//     new:
//       def findUser(token: Token)(claim: JwtClaim): F[Option[User]] =
//         redis
//           .get(token.value)
//           .map:
//             _.flatMap { uJson =>
//               decode[User](uJson).toOption
//             }
