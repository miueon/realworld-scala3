package realworld.db

import doobie.*
import doobie.implicits.*

trait Database[F[_]]:
  def transact[A](c: ConnectionIO[A]): F[A]
