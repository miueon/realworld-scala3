package realworld.auth

import cats.Monad
import cats.syntax.all.*

import io.circe.syntax.*
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtCirce
import pdi.jwt.JwtClaim
import realworld.config.types.JwtAccessTokenKeyConfig
import realworld.config.types.TokenExpiration
import realworld.domain.types.DeriveType
import realworld.effects.GenUUID
import realworld.spec.Token

trait JWT[F[_]]:
  def create: F[Token]

object JWT:
  def make[F[_]: GenUUID: Monad](
      jwtExpire: JwtExpire[F],
      config: JwtAccessTokenKeyConfig,
      exp: TokenExpiration
  ): JWT[F] =
    new:
      def create: F[Token] =
        for
          uuid  <- GenUUID[F].make
          claim <- jwtExpire.expiresIn(JwtClaim(uuid.asJson.noSpaces), exp)
        yield Token(
          JwtCirce.encode(claim, config.value.value, JwtAlgorithm.HS256)
        )
end JWT
