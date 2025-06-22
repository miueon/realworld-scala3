package realworld.db

import cats.~>
import cats.effect.kernel.{MonadCancelThrow, Resource}
import doobie.{ConnectionIO, Transactor}

extension [F[_]: DoobieTx](xa: Transactor[F])
  def transaction: Resource[F, ConnectionIO ~> F] = DoobieTx[F].transaction(xa)
extension [F[_]: DoobieTx: MonadCancelThrow](xa: Transactor[F])
  def transactK[A](ops: ConnectionIO[A]): F[A] =
    DoobieTx[F].transaction(xa).use { fk => fk { ops } }

given connectionIOToF[A, F[_]](using K: ConnectionIO ~> F): Conversion[ConnectionIO[A], F[A]] with
  def apply(c: ConnectionIO[A]): F[A] = K(c)