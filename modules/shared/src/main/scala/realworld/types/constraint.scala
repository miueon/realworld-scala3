package realworld.types

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.any.DescribedAs
import io.github.iltotore.iron.constraint.any.Not
import io.github.iltotore.iron.constraint.string.Blank
import smithy4s.RefinementProvider
import realworld.spec.NonEmptyStringFormat
import smithy4s.Refinement
import realworld.spec.UsernameFormat
import realworld.spec.EmailFormat
import realworld.spec.PasswordFormat

type NonEmptyOrBlank = MinLength[1] & Not[Blank] DescribedAs "should not be empty or blank"

type NonEmptyString = String :| NonEmptyOrBlank
object NonEmptyString extends RefinedTypeOps[String, NonEmptyOrBlank, NonEmptyString]

type UsernameConstraint = MinLength[1] & MaxLength[50] & Not[Blank] DescribedAs
  "should not be blank and should be between 1 and 50 characters"
type Username = String :| UsernameConstraint
object Username extends RefinedTypeOps[String, UsernameConstraint, Username]

type PasswordConstriant = MinLength[8] & MaxLength[100] & Not[Blank] DescribedAs
  "should not be blank and should be between 8 and 100 characters"
type Password = String :| PasswordConstriant
object Password extends RefinedTypeOps[String, PasswordConstriant, Password]

type EmailConstraint =
  Not[Blank] & MinLength[1] & Match["[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"] DescribedAs
    "should not be blank and should be a valid email address"
type Email = String :| EmailConstraint
object Email extends RefinedTypeOps[String, EmailConstraint, Email]

object providers:
  given RefinementProvider[NonEmptyStringFormat, String, NonEmptyString] =
    Refinement.drivenBy[NonEmptyStringFormat](
      NonEmptyString.either(_),
      (a: NonEmptyString) => a
    )

  given RefinementProvider.Simple[smithy.api.Length, NonEmptyString] =
    RefinementProvider.lengthConstraint(_.size)

  given RefinementProvider[UsernameFormat, String, Username] =
    Refinement.drivenBy[UsernameFormat](
      Username.either(_),
      a => a
    )

  given RefinementProvider[EmailFormat, String, Email] =
    Refinement.drivenBy[EmailFormat](
      Email.either(_),
      a => a
    )

  given RefinementProvider[PasswordFormat, String, Password] =
    Refinement.drivenBy[PasswordFormat](
      Password.either(_),
      a => a
    )

end providers
