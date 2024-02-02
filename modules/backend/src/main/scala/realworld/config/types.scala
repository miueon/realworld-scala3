package realworld.config

import scala.concurrent.duration.FiniteDuration

import cats.Show

import io.github.iltotore.iron.cats.given
import realworld.domain.types.DeriveType
import realworld.types.NonEmptyStringR

object types:
  type JwtAccessTokenKeyConfig = JwtAccessTokenKeyConfig.Type
  object JwtAccessTokenKeyConfig extends DeriveType[NonEmptyStringR]:
    given Show[JwtAccessTokenKeyConfig] = derive

  type JwtClaimConfig = JwtClaimConfig.Type
  object JwtClaimConfig extends DeriveType[NonEmptyStringR]:
    given Show[JwtClaimConfig] = derive

  type PasswordSalt = PasswordSalt.Type
  object PasswordSalt extends DeriveType[String]:
    given Show[PasswordSalt] = derive
  // given Decoder.Id[PasswordSalt] = derive

  type TokenExpiration = TokenExpiration.Type
  object TokenExpiration extends DeriveType[FiniteDuration]
end types
