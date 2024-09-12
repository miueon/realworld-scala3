package realworld.types

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.any.DescribedAs
import io.github.iltotore.iron.constraint.any.Not
import io.github.iltotore.iron.constraint.string.Blank
import smithy4s.RefinementProvider
import realworld.spec.NonEmptyStringFormat
import smithy4s.Refinement

type NonEmptyOrBlank = MinLength[1] & Not[Blank] DescribedAs "should not be empty or blank"

type NonEmptyString = String :| NonEmptyOrBlank
object NonEmptyString extends RefinedTypeOps[String, NonEmptyOrBlank, NonEmptyString]

object providers:
  given RefinementProvider[NonEmptyStringFormat, String, NonEmptyString] =
    Refinement.drivenBy[NonEmptyStringFormat](
      NonEmptyString.either(_),
      (a: NonEmptyString) => a
    )

  given RefinementProvider.Simple[smithy.api.Length, NonEmptyString] = 
    RefinementProvider.lengthConstraint(_.size)
end providers
