package realworld.db

import cats.effect.kernel.Resource
import cats.~>
import cats.effect.kernel.MonadCancelThrow

import doobie.ConnectionIO
import doobie.Transactor

extension [F[_]: DoobieTx](xa: Transactor[F])
  def transaction: Resource[F, ConnectionIO ~> F] = DoobieTx[F].transaction(xa)
extension [F[_]: DoobieTx: MonadCancelThrow](xa: Transactor[F])
  def transactK[A](ops: ConnectionIO[A]): F[A] =
    DoobieTx[F].transaction(xa).use { fk => fk { ops } }
