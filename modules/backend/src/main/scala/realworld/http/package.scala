package realworld

import cats.data.*
import cats.MonadError
import cats.effect.kernel.Sync
import cats.syntax.all.*

import realworld.spec.UnprocessableEntity
import realworld.spec.ValidationErrors
import realworld.validation.InvalidField
import realworld.validation.ValidationResult
import smithy4s.Smithy4sThrowable
import cats.MonadThrow
import realworld.spec.Limit
import realworld.spec.Skip

package object http:
  def withValidation[F[_]: Sync, A, B](validated: ValidationResult[A])(
      fn: A => F[B]
  ): F[B] =
    validated.toEither.fold(
      errors =>
        UnprocessableEntity(validationErrorsToResponse((errors)).some)
          .raiseError[F, B],
      fn
    )

  def validationErrorsToResponse(
      nec: NonEmptyChain[InvalidField]
  ) =
    nec.toList.map(e => e.field -> e.errors).toMap

  extension (t: Smithy4sThrowable)
    def raise[F[_]: MonadThrow: Sync, T <: Smithy4sThrowable, A]: F[A] =
      MonadError[F, Throwable].raiseError(t)

  case class Pagination(limit: Limit, skip: Skip)
end http
