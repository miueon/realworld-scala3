package realworld.db

import cats.arrow.FunctionK
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.kernel.Resource.ExitCase.*
import cats.syntax.all.*
import cats.~>

import doobie.*
import doobie.free.connection.setAutoCommit
import doobie.hi.connection.commit
import doobie.hi.connection.rollback
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

trait DoobieTx[F[_]]:
  def transaction(xa: Transactor[F]): Resource[F, ConnectionIO ~> F]

object DoobieTx:
  def apply[F[_]: DoobieTx]: DoobieTx[F] = summon

  given [F[_]: Async: Logger]: DoobieTx[F] with
    def transaction(xa: Transactor[F]): Resource[F, ConnectionIO ~> F] =
      WeakAsync
        .liftK[F, ConnectionIO]
        .flatMap: fk =>
          xa.connect(xa.kernel)
            .flatMap: conn =>
              def log(s: => String) = fk(Logger[F].debug(s"DB: $s"))

              val rawTrans: FunctionK[ConnectionIO, F] =
                FunctionK.lift[ConnectionIO, F] {
                  [T] => (_: ConnectionIO[T]).foldMap(xa.interpret).run(conn)
                }

              Resource
                .makeCase(setAutoCommit(false)):
                  case (_, Succeeded)             => log("COMMIT") *> commit
                  case (_, Canceled | Errored(_)) => log("ROLLBACK") *> rollback
                .mapK(rawTrans)
                .as(rawTrans)
  end given
end DoobieTx
