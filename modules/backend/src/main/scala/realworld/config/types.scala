package realworld.config

import cats.Show
import ciris.Secret
import com.comcast.ip4s.{Host, Port}
import io.github.iltotore.iron.cats.given
import io.github.iltotore.iron.ciris.given
import realworld.domain.types.DeriveType
import realworld.ext.ciris.Decoder

import scala.concurrent.duration.FiniteDuration

object types:
  type JwtAccessTokenKeyConfig = JwtAccessTokenKeyConfig.Type
  object JwtAccessTokenKeyConfig extends DeriveType[NonEmptyStringR]:
    given Decoder.Id[JwtAccessTokenKeyConfig] = derive
    given Show[JwtAccessTokenKeyConfig]       = derive

  type JwtClaimConfig = JwtClaimConfig.Type
  object JwtClaimConfig extends DeriveType[NonEmptyStringR]:
    given Decoder.Id[JwtClaimConfig] = derive
    given Show[JwtClaimConfig]       = derive

  type JwtSecretKeyConfig = JwtSecretKeyConfig.Type
  object JwtSecretKeyConfig extends DeriveType[NonEmptyStringR]:
    given Decoder.Id[JwtSecretKeyConfig] = derive
    given Show[JwtSecretKeyConfig]       = derive

  type PasswordSalt = PasswordSalt.Type
  object PasswordSalt extends DeriveType[String]:
    given Show[PasswordSalt] = derive

  type TokenExpiration = TokenExpiration.Type
  object TokenExpiration extends DeriveType[FiniteDuration]

  type RedisURI = RedisURI.Type
  object RedisURI extends DeriveType[NonEmptyStringR]
  case class RedisConfig(uri: RedisURI)

  case class AppConfig(
      tokenConfig: Secret[JwtAccessTokenKeyConfig],
      passwordSalt: Secret[PasswordSalt],
      tokenExpiration: TokenExpiration,
      postgresSQL: PostgresSQLConfig,
      redis: RedisConfig,
      httpServerConfig: HttpServerConfig
  )

  case class PostgresSQLConfig(
      jdbcUrl: NonEmptyStringR,
      user: NonEmptyStringR,
      password: Secret[NonEmptyStringR]
  )

  case class HttpServerConfig(
      host: Host,
      port: Port
  )

end types
