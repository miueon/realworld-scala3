package realworld.types
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import realworld.spec.Body
import realworld.spec.Description
import realworld.spec.Email
import realworld.spec.Password
import realworld.spec.Title
import realworld.spec.Username
import realworld.spec.TagName
import realworld.spec.Bio
import realworld.spec.ImageUrl
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

case class LoginCredential(email: Email, password: Password) extends FormRecord
case class RegisterCredential(username: Username, email: Email, password: Password)
    extends FormRecord
case class ArticleForm(title: Title, description: Description, body: Body, tagList: List[TagName])
    extends FormRecord
case class UserSettings(
    email: Option[Email] = None,
    username: Option[Username] = None,
    password: Option[Password] = None,
    bio: Option[Bio] = None,
    image: Option[ImageUrl] = None
) extends FormRecord
