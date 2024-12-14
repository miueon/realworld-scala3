package realworld.effects

import cats.Functor
import cats.effect.kernel.Sync
import cats.syntax.functor.*
import smithy4s.Timestamp

import java.time.Instant

trait Time[F[_]: Functor]:
  def now: F[Instant]
  def timestamp: F[Timestamp] = now.map(Timestamp.fromInstant)

object Time:
  def apply[F[_]: Time]: Time[F] = summon

  given [F[_]: Sync]: Time[F] with
    def now: F[Instant] = Sync[F].delay(Instant.now())
