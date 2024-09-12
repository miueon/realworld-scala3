package realworld.http
import cats.data.*
import cats.syntax.all.*

package object validation:
  trait InvalidField:
    def errors: List[String]
    def field: String

  type ValidationResult[A] = ValidatedNec[InvalidField, A]

  def toInvalidField[F <: InvalidField](
      nec: NonEmptyChain[String],
      mkInvalidField: List[String] => F
  ): NonEmptyChain[F] =
    NonEmptyChain.one(mkInvalidField(nec.toList))

  object validators:
    type ValidatorResult[A] = ValidatedNec[String, A]
    def notBlank(s: String): ValidatorResult[String] =
      if s.nonEmpty then s.validNec else "can't be blank".invalidNec

    def min(s: String, minSize: Int): ValidatorResult[String] =
      if s.size >= minSize then s.validNec
      else s"is too short (minimum is $minSize character)".invalidNec

    def max(s: String, maxSize: Int): ValidatorResult[String] =
      if s.size <= maxSize then s.validNec
      else s"is too long (maximum is $maxSize character)".invalidNec

    val emailPattern = ".+@.+\\..+".r
    def looksLikeEmail(s: String): ValidatorResult[String] =
      if emailPattern.matches(s) then s.validNec else "is invalid".invalidNec
  end validators
end validation
