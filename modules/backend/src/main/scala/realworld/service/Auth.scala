package realworld.service

import cats.Functor
import cats.Monad
import cats.MonadThrow
import cats.syntax.all.*

import dev.profunktor.redis4cats.RedisCommands
import io.circe.Codec
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.Json
import io.circe.parser.decode
import io.circe.syntax.*
import pdi.jwt.JwtClaim
import realworld.auth.Crypto
import realworld.auth.JWT
import realworld.config.types.TokenExpiration
import realworld.domain.users.Users.username
import realworld.spec.ForbiddenError
import realworld.spec.LoginUserInputData
import realworld.spec.NotFoundError
import realworld.spec.RegisterUserData
import realworld.spec.Token
import realworld.spec.User
import smithy4s.Document
import smithy4s.schema.Schema

trait Auth[F[_]]:
  def login(user: LoginUserInputData): F[User]
  def register(user: RegisterUserData): F[User]

trait UserAuth[F[_]]:
  def findUser(token: Token)(claim: JwtClaim): F[Option[User]]

object Auth:
  def make[F[_]: MonadThrow](
      tokenExpiration: TokenExpiration,
      jwt: JWT[F],
      users: Users[F],
      redis: RedisCommands[F, String, String],
      crypto: Crypto
  ): Auth[F] =
    new:
      private val TokenExpiration = tokenExpiration.value
      def login(user: LoginUserInputData): F[User] =
        users
          .findByEmail(user.email)
          .flatMap:
            case None => NotFoundError().raiseError[F, User]
            case Some(userWithPassword)
                if userWithPassword.password =!= crypto.encrypt(
                  user.password
                ) =>
              ForbiddenError().raiseError[F, User]
            case Some(userWithPassword) =>
              redis
                .get(user.email.value)
                .flatMap:
                  case Some(t) =>
                    userWithPassword.user.copy(token = Token(t).some).pure[F]
                  case None =>
                    jwt.create.flatMap: t =>
                      redis.setEx(
                        t.value,
                        userWithPassword.user.asJson.noSpaces,
                        TokenExpiration
                      )
                      redis.setEx(
                        user.email.value,
                        t.value,
                        TokenExpiration
                      )
                      userWithPassword.user.pure[F]
      end login

      def register(user: RegisterUserData): F[User] =
        users
          .findByEmail(user.email)
          .flatMap:
            case Some(_) => ForbiddenError().raiseError[F, User]
            case None =>
              val encryptedPassword = crypto.encrypt(user.password)
              for
                _   <- users.create(user, encryptedPassword)
                jwt <- jwt.create
              yield User(
                email = user.email,
                username = user.username,
                token = jwt.some
              )
      end register
end Auth

object UserAuth:
  def make[F[_]: Functor](
      redis: RedisCommands[F, String, String]
  ): UserAuth[F] =
    new:
      def findUser(token: Token)(claim: JwtClaim): F[Option[User]] =
        redis
          .get(token.value)
          .map:
            _.flatMap { uJson =>
              decode[User](uJson).toOption
            }
