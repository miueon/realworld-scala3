package realworld.db

import cats.effect.kernel.{MonadCancelThrow, Resource}
import cats.~>
import doobie.{ConnectionIO, Transactor}

extension [F[_]: DoobieTx](xa: Transactor[F])
  def transaction: Resource[F, ConnectionIO ~> F] = DoobieTx[F].transaction(xa)
extension [F[_]: DoobieTx: MonadCancelThrow](xa: Transactor[F])
  def transactK[A](ops: ConnectionIO[A]): F[A] =
    DoobieTx[F].transaction(xa).use { fk => fk { ops } }

implicit def connectionIOToF[A, F[_]](c: ConnectionIO[A])(using K: ConnectionIO ~> F): F[A] =
  K(c)
