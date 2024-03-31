package realworld.components.widgets

import _root_.utils.Utils.or
import _root_.utils.Utils.validateIf
import _root_.utils.Utils.writerNTF
import com.raquo.airstream.core.Observer
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.modifiers.EventListener
import com.raquo.laminar.nodes.ReactiveHtmlElement
import monocle.syntax.all.*
import org.scalajs.dom.Event
import org.scalajs.dom.HTMLInputElement
import realworld.api.Api
import realworld.components.Component
import realworld.spec.Email
import realworld.spec.LoginUserInputData
import realworld.spec.Password
import realworld.spec.UnprocessableEntity
import realworld.spec.ValidationErrors
import realworld.types.InputStateConfig
import realworld.types.InputStateConfig.inputStateMod
import realworld.types.InputType
import realworld.types.validation.GenericError
import typings.std.stdStrings.em

import scala.concurrent.ExecutionContext.Implicits.global
import realworld.types.GenericFormField
import realworld.types.FieldType

case class Credentials(
    email: Email,
    password: Password
)

final case class CredentialForm(api: Api) extends Component:
  val credential: Var[Credentials]     = Var(Credentials(Email(""), Password("")))
  val emailWriter: Observer[String]    = credential.writerNTF(Email, _.focus(_.email).optic)
  val passwordWriter: Observer[String] = credential.writerNTF(Password, _.focus(_.password).optic)
  val errors: Var[GenericError]        = Var(Map())
  val handler = Observer[Credentials] { case Credentials(email, password) =>
    api
      .future(
        _.users
          .loginUser(
            LoginUserInputData(
              email,
              password
            )
          )
          .attempt
      )
      .collect {
        case Left(UnprocessableEntity(Some(e))) => errors.set(e)
        case Right(_)                           => ???
      }
  }

  override def fragement: Seq[HtmlElement] =
    testGF(
      errors.signal,
      onSubmit.preventDefault.mapTo(credential.now()) --> handler,
      "Sign in",
      List(
        GenericFormField(
          "Email",
          InputType.Text,
          "Email",
          FieldType.Input,
          true,
          controlled(
            value <-- credential.signal.map(_.email.value),
            onInput.mapToValue --> emailWriter
          )
        ),
        GenericFormField(
          "Password",
          InputType.Password,
          "Password",
          FieldType.Input,
          false,
          controlled(
            value <-- credential.signal.map(_.password.value),
            onInput.mapToValue --> passwordWriter
          )
        )
      )
    )
    ???
  end fragement

  def testGF(
      esg: Signal[GenericError],
      onSubmit: EventListener[Event, Credentials],
      submitButtonText: String,
      fields: List[GenericFormField]
  ) =
    val s_disabled = esg.map(_.nonEmpty)
    List(
      Errors(esg),
      form(
        onSubmit,
        fieldSet(
          fields.map { field =>
            field.filedType match
              case FieldType.Input =>
                Input(
                  field.tpe,
                  field.placeholder,
                  s_disabled,
                  field.isLarge,
                  field.controlled
                )
              case _ => ???
          },
          button(
            cls := "btn btn-lg btn-primary pull-xs-right",
            submitButtonText
          )
        )
        // onSubmit.preventDefault.mapTo(credential.now()) --> handler,
        // Input(
        //   InputType.Password,
        //   "Password",
        //   errors.map(_.isEmpty),
        //   false,
        //   passwordWriter,
        //   controlled = controlled(
        //     value <-- credential.signal.map(_.password.value),
        //     onInput.mapToValue --> passwordWriter
        //   )
        // )
      )
    )
  end testGF

  def Input(
      inputType: InputType,
      _placeholder: String,
      s_disabled: Signal[Boolean],
      isLarge: Boolean,
      controlled: Mod[Input]
  ) =
    fieldSet(
      cls := "form-group",
      input(
        cls         := s"form-control${if isLarge then " form-control-lg" else ""}",
        tpe         := inputType.value,
        placeholder := _placeholder,
        disabled <-- s_disabled,
        controlled
      )
    )
  end Input
end CredentialForm

// final case class FormGroup(
//     inputType: InputType,
//     _placeholder: String,
//     isDisabled: Boolean,
//     isLarge: Boolean,
//     onChange: Mod[ReactiveHtmlElement[HTMLInputElement]]
// ) extends Component:
//   // def test(): Mod[ReactiveHtmlElement[HTMLInputElement]] = Binder { el =>
//   //   val sing = Var("")
//   //   (onInput.mapToValue --> sing.writer).bind(el)
//   // }
//   override def body: HtmlElement =
//     fieldSet(
//       cls := "form-group",
//       input(
//         cls         := s"form-control${if isLarge then " form-control-lg" else ""}",
//         tpe         := inputType.value,
//         placeholder := _placeholder,
//         disabled    := isDisabled,
//         onChange
//       )
//     )
//     ???
// end FormGroup
