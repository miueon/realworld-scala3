package realworld.auth

import java.time.Clock

import cats.effect.kernel.Sync
import cats.syntax.all.*

import pdi.jwt.JwtClaim
import realworld.config.types.TokenExpiration
import realworld.effects.JwtClock

trait JwtExpire[F[_]]:
  def expiresIn(claim: JwtClaim, exp: TokenExpiration): F[JwtClaim]

object JwtExpire:
  def make[F[_]: Sync]: F[JwtExpire[F]] =
    JwtClock[F].utc.map: jClock =>
      given Clock = jClock
      new JwtExpire[F]:
        def expiresIn(claim: JwtClaim, exp: TokenExpiration): F[JwtClaim] =
          Sync[F].delay(claim.issuedNow.expiresIn(exp.value.toMillis))
