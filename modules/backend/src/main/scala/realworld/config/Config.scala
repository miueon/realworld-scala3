package realworld.config
import cats.effect.kernel.Async
import cats.syntax.all.*
import ciris.*
import com.comcast.ip4s.*
import io.github.iltotore.iron.autoRefine
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.ciris.given
import realworld.config.types.{
  AppConfig,
  HttpServerConfig,
  JwtAccessTokenKeyConfig,
  PasswordSalt,
  PostgresSQLConfig,
  RedisConfig,
  RedisURI,
  TokenExpiration
}

import scala.concurrent.duration.*

object Config:
  def load[F[_]: Async]: F[AppConfig] =
    env("SC_APP_ENV")
      .as[AppEnvironment]
      .flatMap {
        case AppEnvironment.Test =>
          default("jdbc:postgresql://localhost:5432/realworld", RedisURI("redis://localhost"))
        case AppEnvironment.Prod =>
          default("jdbc:postgresql://postgres:5432/realworld", RedisURI("redis://redis"))
      }
      .load[F]

  private def default[F[_]](
      postgressUri: NonEmptyStringR,
      redisUri: RedisURI
  ): ConfigValue[F, AppConfig] =
    (
      env("SC_ACCESS_TOKEN_KEY").as[JwtAccessTokenKeyConfig].secret,
      env("SC_PASSWORD_SALT").as[PasswordSalt].secret,
      env("SC_POSTGRES_PASSWORD").as[NonEmptyStringR].secret
    ).parMapN { (accessToken, passwordSalt, postgressPassword) =>
      AppConfig(
        accessToken,
        passwordSalt,
        TokenExpiration(30.minutes),
        PostgresSQLConfig(
          jdbcUrl = postgressUri,
          user = "postgres",
          password = postgressPassword
        ),
        RedisConfig(redisUri),
        HttpServerConfig(
          host = host"0.0.0.0",
          port = port"8088"
        )
      )
    }
end Config
