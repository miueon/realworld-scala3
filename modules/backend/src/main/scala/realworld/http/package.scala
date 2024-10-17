package realworld

import cats.syntax.all.*
import cats.{MonadError, MonadThrow}
import realworld.spec.{Limit, Skip}
import smithy4s.Smithy4sThrowable

package object http:
  extension (t: Smithy4sThrowable)
    def raise[F[_]: MonadThrow, T <: Smithy4sThrowable, A]: F[A] =
      MonadError[F, Throwable].raiseError(t)

  case class Pagination(limit: Limit, skip: Skip)
  object Pagination:
    def apply(limit: Option[Limit], skip: Option[Skip]): Pagination =
      Pagination(limit.getOrElse(Limit(10)), skip.getOrElse(Skip(0)))
end http
