package realworld.effects

import cats.ApplicativeThrow
import cats.effect.kernel.Sync

import java.util.UUID

trait GenUUID[F[_]]:
  def make: F[UUID]
  def read(str: String): F[UUID]

object GenUUID:
  def apply[F[_]: GenUUID]: GenUUID[F] = summon[GenUUID[F]]

  given forSync[F[_]: Sync]: GenUUID[F] with
    def make: F[UUID] =
      Sync[F].delay(UUID.randomUUID())
    def read(str: String): F[UUID] =
      ApplicativeThrow[F].catchNonFatal(UUID.fromString(str))
