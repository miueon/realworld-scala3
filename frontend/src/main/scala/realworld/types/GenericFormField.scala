package realworld.types
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
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
    name: String,
    tpe: InputType = InputType.Text,
    placeholder: String = "",
    filedType: FieldType = FieldType.Input,
    isLarge: Boolean = true,
    controlled: Mod[Input | TextArea]
)
