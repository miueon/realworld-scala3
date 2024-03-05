package realworld

import cats.MonadError
import cats.MonadThrow
import cats.syntax.all.*
import realworld.spec.Limit
import realworld.spec.Skip
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
