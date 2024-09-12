package realworld.http.validation
import cats.syntax.all.*
import realworld.http.validation.validators.*
import realworld.spec.Email
import realworld.spec.Password

case class InvalidEmail(errors: List[String], field: String = "email")       extends InvalidField
case class InvalidPassword(errors: List[String], field: String = "password") extends InvalidField
case class InvalidUsername(errors: List[String], field: String = "username") extends InvalidField

def validEmail(email: Email): ValidationResult[Email] =
  val trimmed = email.value.trim()
  (notBlank(trimmed), max(trimmed, 50), looksLikeEmail(trimmed))
    .mapN({ case t => t._1 })
    .leftMap(toInvalidField(_, InvalidEmail(_)))
    .map(Email(_))

// def validPassword(password: Password): ValidationResult[Password] =
//   val trimmed = password.value.trim()
//   (notBlank(trimmed), max(trimmed, 50), min(trimmed, 8))
//     .mapN({ case t => t._1 })
//     .leftMap(toInvalidField(_, InvalidPassword(_)))
//     .map(Password(_))

// def validUsername(username: Username): ValidationResult[Username] =
//   val trimmed = username.value.trim()
//   (notBlank(trimmed), min(trimmed, 1), max(trimmed, 25))
//     .mapN({ case t => t._1 })
//     .leftMap(toInvalidField(_, InvalidUsername(_)))
//     .map(Username(_))
