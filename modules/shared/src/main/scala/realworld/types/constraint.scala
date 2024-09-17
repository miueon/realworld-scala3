package realworld.types

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.constraint.any.DescribedAs
import io.github.iltotore.iron.constraint.any.Not
import io.github.iltotore.iron.constraint.string.Blank
import smithy4s.RefinementProvider
import smithy4s.Refinement
import realworld.spec.UsernameFormat
import realworld.spec.EmailFormat
import realworld.spec.PasswordFormat
import realworld.spec.TitleFormat
import realworld.spec.DescriptionFormat
import realworld.spec.BodyFormat
import realworld.spec.TagNameFormat
import realworld.spec.CommentBodyFormat
import realworld.spec.NonEmptyListFormat
import realworld.spec.ImageUrlFormat

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

type NotBlankConstriant = MinLength[1] & Not[Blank] DescribedAs "should not be blank"
type TitleConstraint    = NotBlankConstriant
type Title              = String :| TitleConstraint
object Title extends RefinedTypeOps[String, TitleConstraint, Title]

type DescriptionConstraint = NotBlankConstriant
type Description           = String :| DescriptionConstraint
object Description extends RefinedTypeOps[String, DescriptionConstraint, Description]

type BodyConstraint = NotBlankConstriant
type Body           = String :| BodyConstraint
object Body extends RefinedTypeOps[String, BodyConstraint, Body]

type TagNameConstraint = NotBlankConstriant
type TagName           = String :| TagNameConstraint
object TagName extends RefinedTypeOps[String, TagNameConstraint, TagName]

type CommentBodyConstraint = NotBlankConstriant
type CommentBody           = String :| CommentBodyConstraint
object CommentBody extends RefinedTypeOps[String, CommentBodyConstraint, CommentBody]

type ImageUrlConstraint = Not[Blank] & MinLength[1] DescribedAs
  "should not be blank and should be at least 1 character"
type ImageUrl = String :| ImageUrlConstraint
object ImageUrl extends RefinedTypeOps[String, ImageUrlConstraint, ImageUrl]

opaque type NonEmptyList = [A] =>> List[A] :| MinLength[1] DescribedAs "should not be empty"
object NonEmptyList:
  given rtc[A]: RefinedTypeOps[List[A], MinLength[1], NonEmptyList[A]] =
    new RefinedTypeOps[List[A], MinLength[1], NonEmptyList[A]] {}

object providers:
  given RefinementProvider[UsernameFormat, String, Username] =
    Refinement.drivenBy[UsernameFormat](Username.either(_), identity)

  given RefinementProvider[EmailFormat, String, Email] =
    Refinement.drivenBy[EmailFormat](Email.either(_), identity)

  given RefinementProvider[PasswordFormat, String, Password] =
    Refinement.drivenBy[PasswordFormat](Password.either(_), identity)

  given RefinementProvider[TitleFormat, String, Title] =
    Refinement.drivenBy[TitleFormat](Title.either(_), identity)

  given RefinementProvider[DescriptionFormat, String, Description] =
    Refinement.drivenBy[DescriptionFormat](Description.either(_), identity)

  given RefinementProvider[BodyFormat, String, Body] =
    Refinement.drivenBy[BodyFormat](Body.either(_), identity)

  given RefinementProvider[TagNameFormat, String, TagName] =
    Refinement.drivenBy[TagNameFormat](TagName.either(_), identity)

  given RefinementProvider[CommentBodyFormat, String, CommentBody] =
    Refinement.drivenBy[CommentBodyFormat](CommentBody.either(_), identity)

  given RefinementProvider[ImageUrlFormat, String, ImageUrl] =
    Refinement.drivenBy[ImageUrlFormat](ImageUrl.either(_), identity)

  given [A]: RefinementProvider[NonEmptyListFormat, List[A], NonEmptyList[A]] =
    Refinement.drivenBy[NonEmptyListFormat](
      NonEmptyList.rtc[A].either,
      (a: NonEmptyList[A]) => a.value
    )

  given [A]: RefinementProvider.Simple[smithy.api.Length, NonEmptyList[A]] =
    RefinementProvider.lengthConstraint(_.value.length)
end providers
