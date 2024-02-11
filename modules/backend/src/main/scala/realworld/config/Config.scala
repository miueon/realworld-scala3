package realworld.config
import scala.concurrent.duration.*

import cats.effect.kernel.Async
import cats.syntax.all.*

import ciris.*
import com.comcast.ip4s.*
import io.github.iltotore.iron.autoRefine
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.ciris.given
import realworld.config.types.AppConfig
import realworld.config.types.HttpServerConfig
import realworld.config.types.JwtAccessTokenKeyConfig
import realworld.config.types.JwtClaimConfig
import realworld.config.types.JwtSecretKeyConfig
import realworld.config.types.PasswordSalt
import realworld.config.types.PostgresSQLConfig
import realworld.config.types.RedisConfig
import realworld.config.types.RedisURI
import realworld.config.types.TokenExpiration
import doobie.util.pos

object Config:
  def load[F[_]: Async]: F[AppConfig] =
    env("SC_APP_ENV")
      .as[AppEnvironment]
      .flatMap {
        case AppEnvironment.Test => default(RedisURI("redis://localhost"))
        case AppEnvironment.Prod => default(RedisURI("redis://redis"))
      }
      .load[F]

  private def default[F[_]](redisUri: RedisURI): ConfigValue[F, AppConfig] =
    (
      env("SC_JWT_SECRET_KEY").as[JwtSecretKeyConfig].secret,
      env("SC_JWT_CLAIM").as[JwtClaimConfig].secret,
      env("SC_ACCESS_TOKEN_KEY").as[JwtAccessTokenKeyConfig].secret,
      env("SC_PASSWORD_SALT").as[PasswordSalt].secret,
      env("SC_POSTGRESS_PASSWORD").as[NonEmptyStringR].secret
    ).parMapN {
      (jwtSecretKey, jwtClaim, accessToken, passwordSalt, postgressPassword) =>
        AppConfig(
          accessToken,
          passwordSalt,
          TokenExpiration(30.minutes),
          PostgresSQLConfig(
            jdbcUrl = "jdbc:postgresql://localhost:5432/realworld",
            user = "postgres",
            password = postgressPassword
          ),
          RedisConfig(redisUri),
          HttpServerConfig(
            host = host"0.0.0.0",
            port = port"8080"
          )
        )
    }
end Config
