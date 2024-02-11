package realworld
package validation
import cats.data.Validated.*
import cats.data.*
import cats.syntax.all.*

import realworld.spec.Password
import realworld.spec.UpdateUserData
import realworld.spec.Username
import realworld.spec.RegisterUserData
import smithy4s.Newtype

enum InvalidField(val errors: List[String], val field: String):
  case InvalidEmail(override val errors: List[String]) extends InvalidField(errors, "email")
  case InvalidPassword(override val errors: List[String])
      extends InvalidField(errors, "password")
  case InvalidUsername(override val errors: List[String])
      extends InvalidField(errors, "username")
  case InvalidTitle(override val errors: List[String]) extends InvalidField(errors, "title")
  case InvalidDescription(override val errors: List[String])
      extends InvalidField(errors, "description")
  case InvalidBody(override val errors: List[String]) extends InvalidField(errors, "body")

type ValidationResult[A] = ValidatedNec[InvalidField, A]

object validators:
  type ValidatorResult[A] = ValidatedNec[String, A]

  def notBlank(s: String): ValidatorResult[String] =
    if s.nonEmpty then s.validNec else "can't be blank".invalidNec

  def min(s: String, minSize: Int): ValidatorResult[String] =
    if s.size >= minSize then s.validNec
    else s"is too short (minimum is $minSize character)".invalidNec

  def max(s: String, maxSize: Int): ValidatorResult[String] =
    if s.size <= maxSize then s.validNec
    else s"is too long (maximum is $maxSize charactoer)".invalidNec

def validateUpdateUserBody(
    body: UpdateUserData
): ValidationResult[UpdateUserData] =
  (
    body.email.validNec,
    body.username.traverse(validUsername),
    body.password.traverse(validPassword),
    body.bio.validNec,
    body.image.validNec
  )
    .mapN(UpdateUserData.apply)

def validateRegisterUserBody(
    body: RegisterUserData
): ValidationResult[RegisterUserData] = (
  validUsername(body.username),
  body.email.validNec,
  validPassword(body.password)
).mapN(RegisterUserData.apply)

import validators.*
import InvalidField.*

def toInvalidField[F <: InvalidField, A](
    nec: NonEmptyChain[A],
    mkInvalidField: List[A] => F
): NonEmptyChain[F] = NonEmptyChain.one(mkInvalidField(nec.toList))

def validUsername(username: Username): ValidationResult[Username] =
  notBlank(username.value.trim)
    .map(Username(_))
    .leftMap(
      toInvalidField(_, InvalidUsername(_))
    )

def validPassword(password: Password): ValidationResult[Password] =
  notBlank(password.value.trim)
    .map(Password(_))
    .leftMap(toInvalidField(_, InvalidPassword(_)))
