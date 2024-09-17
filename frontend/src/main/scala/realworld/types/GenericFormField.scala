package realworld.types
import cats.data.EitherNec
import cats.syntax.all.*
import com.raquo.laminar.api.L.*
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.string.Blank
import realworld.spec.Bio
import realworld.spec.LoginUserInputData
import realworld.spec.RegisterUserData
import realworld.spec.UnprocessableEntity
import realworld.validation.InvalidField.*
import utils.Utils.*
import realworld.spec.UpdateUserData
import realworld.spec.CreateArticleData
import realworld.spec.UpdateArticleData
import scala.reflect.ClassTag

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
    s_tags: Option[Signal[List[String]]] = None
)
sealed trait FormRecord

trait ValidatedToReqData[V, A: ClassTag]:
  extension (v: V) def validatedToReqData: Either[UnprocessableEntity, A]

def process[L, R](optEither: Option[EitherNec[L, R]]): EitherNec[L, Option[R]] =
  optEither match
    case None         => Right(None)
    case Some(either) => either.map(Some(_))

case class LoginCredential(email: String = "", password: String = "") extends FormRecord
// ValidatedToReqData[LoginUserInputData]:
object LoginCredential:
  given ValidatedToReqData[LoginCredential, LoginUserInputData] with
    extension (v: LoginCredential)
      def validatedToReqData: Either[UnprocessableEntity, LoginUserInputData] =
        (
          v.email.refineEntity[EmailConstraint](InvalidEmail(_)),
          v.password.refineEntity[PasswordConstriant](InvalidPassword(_))
        ).parMapN(LoginUserInputData.apply).toUnprocessable

case class RegisterCredential(username: String = "", email: String = "", password: String = "")
    extends FormRecord
object RegisterCredential:
  given ValidatedToReqData[RegisterCredential, RegisterUserData] with
    extension (v: RegisterCredential)
      def validatedToReqData: Either[UnprocessableEntity, RegisterUserData] =
        (
          v.username.refineEntity[UsernameConstraint](InvalidUsername(_)),
          v.email.refineEntity[EmailConstraint](InvalidEmail(_)),
          v.password.refineEntity[PasswordConstriant](InvalidPassword(_))
        ).parMapN(RegisterUserData.apply).toUnprocessable

case class ArticleForm(
    title: Option[String] = None,
    description: Option[String] = None,
    body: Option[String] = None,
    tagList: List[String] = List()
) extends FormRecord
object ArticleForm:
  given c: ValidatedToReqData[ArticleForm, CreateArticleData] with
    extension (v: ArticleForm)
      def validatedToReqData: Either[UnprocessableEntity, CreateArticleData] =
        (
          v.title.getOrElse("").refineEntity[TitleConstraint](InvalidTitle(_)),
          v.description.getOrElse("").refineEntity[DescriptionConstraint](InvalidDescription(_)),
          v.body.getOrElse("").refineEntity[BodyConstraint](InvalidBody(_)),
          v.tagList.map(_.refineEntity[TagNameConstraint](InvalidTag(_))).sequence
        ).parMapN(CreateArticleData.apply).toUnprocessable

  given u: ValidatedToReqData[ArticleForm, UpdateArticleData] with
    extension (v: ArticleForm)
      def validatedToReqData: Either[UnprocessableEntity, UpdateArticleData] =
        (
          v.tagList.map(_.refineEntity[TagNameConstraint](InvalidTag(_))).sequence,
          process(v.title.map(_.refineEntity[TitleConstraint](InvalidTitle(_)))),
          process(v.description.map(_.refineEntity[DescriptionConstraint](InvalidDescription(_)))),
          process(v.body.map(_.refineEntity[BodyConstraint](InvalidBody(_))))
        ).parMapN(UpdateArticleData.apply).toUnprocessable

end ArticleForm
case class UserSettings(
    email: Option[String] = None,
    username: Option[String] = None,
    password: Option[String] = None,
    bio: Option[Bio] = None,
    image: Option[String] = None
) extends FormRecord
object UserSettings:
  given ValidatedToReqData[UserSettings, UpdateUserData] with
    extension (v: UserSettings)
      def validatedToReqData: Either[UnprocessableEntity, UpdateUserData] =
        (
          process(v.email.map(_.refineEntity[EmailConstraint](InvalidEmail(_)))),
          process(v.username.map(_.refineEntity[UsernameConstraint](InvalidUsername(_)))),
          process(v.password.map(_.refineEntity[PasswordConstriant](InvalidPassword(_)))),
          v.bio.rightNec,
          process(v.image.map(_.refineEntity[ImageUrlConstraint](InvalidImageUrl(_))))
        )
          .parMapN(UpdateUserData.apply)
          .toUnprocessable
  end given
end UserSettings
