package realworld.effects

import cats.effect.kernel.Sync

import java.time.Instant
import smithy4s.Timestamp
import cats.Functor
import cats.syntax.functor.*

trait Time[F[_]: Functor]:
  def now: F[Instant]
  def timestamp: F[Timestamp] = now.map(Timestamp.fromInstant)

object Time:
  def apply[F[_]: Time]: Time[F] = summon

  given [F[_]: Sync]: Time[F] with
    def now: F[Instant] = Sync[F].delay(Instant.now())
