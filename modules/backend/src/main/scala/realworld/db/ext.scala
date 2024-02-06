package realworld.db

import cats.effect.kernel.Resource
import cats.~>

import doobie.ConnectionIO
import doobie.Transactor

extension [F[_]: DoobieTx](xa: Transactor[F])
  def transaction: Resource[F, ConnectionIO ~> F] = DoobieTx[F].transaction(xa)
