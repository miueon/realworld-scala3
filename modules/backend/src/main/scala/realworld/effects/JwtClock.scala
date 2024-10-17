package realworld.effects

import cats.effect.kernel.Sync

import java.time.Clock

trait JwtClock[F[_]]:
  def utc: F[Clock]

object JwtClock:
  def apply[F[_]: JwtClock] = summon[JwtClock[F]]

  given forClock[F[_]: Sync]: JwtClock[F] with
    def utc: F[Clock] = Sync[F].delay(Clock.systemUTC())
