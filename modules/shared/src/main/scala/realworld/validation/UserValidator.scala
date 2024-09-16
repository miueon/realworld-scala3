package realworld.validation
import cats.syntax.all.*
// import realworld.validation.validators.*

case class InvalidEmail(error: String, field: String = "email")       extends InvalidField
case class InvalidPassword(error: String, field: String = "password") extends InvalidField
case class InvalidUsername(error: String, field: String = "username") extends InvalidField

// def validEmail(email: Email): ValidationResult[Email] =
//   val trimmed = email.value.trim()
//   (notBlank(trimmed), max(trimmed, 50), looksLikeEmail(trimmed))
//     .mapN({ case t => t._1 })
//     .leftMap(toInvalidField(_, InvalidEmail(_)))
//     .map(Email(_))

// def validPassword(password: Password): ValidationResult[Password] =
//   val trimmed = password.value.trim()
//   (notBlank(trimmed), max(trimmed, 50), min(trimmed, 8))
//     .mapN({ case t => t._1 })
//     .leftMap(toInvalidField(_, InvalidPassword(_)))
//     .map(Password(_))
