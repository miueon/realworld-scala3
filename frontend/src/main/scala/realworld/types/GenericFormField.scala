package realworld.types
import cats.data.EitherNec
import cats.syntax.all.*
import com.raquo.laminar.api.L.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.string.Blank
import realworld.spec.Bio
import realworld.spec.Body
import realworld.spec.Description
import realworld.spec.ImageUrl
import realworld.spec.LoginUserInputData
import realworld.spec.RegisterUserData
import realworld.spec.TagName
import realworld.spec.Title
import realworld.spec.UnprocessableEntity
import realworld.validation.InvalidEmail
import realworld.validation.InvalidPassword
import realworld.validation.InvalidUsername
import utils.Utils.*
import realworld.spec.UpdateUserData

opaque type InputType = String
object InputType:
  val Text: InputType                        = "text"
  val Password: InputType                    = "password"
  extension (i: InputType) def value: String = i

enum FieldType:
  case Input
  case Textarea
  case Lst

case class GenericFormField(
    tpe: InputType = InputType.Text,
    placeholder: String = "",
    fieldType: FieldType = FieldType.Input,
    isLarge: Boolean = true,
    controlled: Mod[Input | TextArea],
    rows: Option[Int] = None,
    s_tags: Option[Signal[List[TagName]]] = None
)
sealed trait FormRecord

trait ValidatedToReqData[A]:
  def validatedToReqData: Either[UnprocessableEntity, A]

case class LoginCredential(email: String = "", password: String = "")
    extends FormRecord,
      ValidatedToReqData[LoginUserInputData]:
  def validatedToReqData: Either[UnprocessableEntity, LoginUserInputData] =
    (
      email.refineEntity[EmailConstraint](InvalidEmail(_)),
      password.refineEntity[PasswordConstriant](InvalidPassword(_))
    ).parMapN(LoginUserInputData.apply).toUnprocessable
case class RegisterCredential(username: String = "", email: String = "", password: String = "")
    extends FormRecord,
      ValidatedToReqData[RegisterUserData]:
  def validatedToReqData: Either[UnprocessableEntity, RegisterUserData] =
    (
      username.refineEntity[UsernameConstraint](InvalidUsername(_)),
      email.refineEntity[EmailConstraint](InvalidEmail(_)),
      password.refineEntity[PasswordConstriant](InvalidPassword(_))
    ).parMapN(RegisterUserData.apply).toUnprocessable

case class ArticleForm(
    title: Title = Title(""),
    description: Description = Description(""),
    body: Body = Body(""),
    tagList: List[TagName] = List()
) extends FormRecord:
  def validatedToReqData[A]: Either[UnprocessableEntity, A] = ???
case class UserSettings(
    email: Option[String] = None,
    username: Option[String] = None,
    password: Option[String] = None,
    bio: Option[Bio] = None,
    image: Option[ImageUrl] = None
) extends FormRecord,
      ValidatedToReqData[UpdateUserData]:
  def validatedToReqData: Either[UnprocessableEntity, UpdateUserData] =
    def process[L, R](optEither: Option[EitherNec[L, R]]): EitherNec[L, Option[R]] = optEither match
      case None         => Right(None)
      case Some(either) => either.map(Some(_))
    (
      process(email.map(_.refineEntity[EmailConstraint](InvalidEmail(_)))),
      process(username.map(_.refineEntity[UsernameConstraint](InvalidUsername(_)))),
      process(password.map(_.refineEntity[PasswordConstriant](InvalidPassword(_)))),
      ???,
      ???
    )
    .parMapN(UpdateUserData.apply).toUnprocessable
end UserSettings

