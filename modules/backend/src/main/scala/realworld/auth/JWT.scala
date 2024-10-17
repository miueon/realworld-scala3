package realworld.auth

import cats.Monad
import cats.syntax.all.*
import io.circe.syntax.*
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import realworld.config.types.{JwtAccessTokenKeyConfig, TokenExpiration}
import realworld.effects.GenUUID
import realworld.spec.Token

trait JWT[F[_]]:
  def create: F[Token]
  def validate(token: Token): F[Boolean]

object JWT:
  def make[F[_]: GenUUID: Monad](
      jwtExpire: JwtExpire[F],
      config: JwtAccessTokenKeyConfig,
      exp: TokenExpiration
  ): JWT[F] =
    new:
      private val alg = JwtAlgorithm.HS256
      def create: F[Token] =
        for
          uuid  <- GenUUID[F].make
          claim <- jwtExpire.expiresIn(JwtClaim(uuid.asJson.noSpaces), exp)
        yield Token(
          JwtCirce.encode(claim, config.value, alg)
        )

      def validate(token: Token): F[Boolean] =
        JwtCirce
          .isValid(token.value, config.value, Seq(alg))
          .pure[F]
end JWT
