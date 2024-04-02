package realworld.components.pages

import com.raquo.laminar.api.L.{*}
import monocle.syntax.all.*
import realworld.api.Api
import realworld.components.Component
import realworld.components.widgets.ContainerPage
import realworld.components.widgets.CredentialForm
import realworld.spec.Email
import realworld.spec.LoginUserInputData
import realworld.spec.Password
import realworld.spec.UnprocessableEntity
import realworld.types.FieldType
import realworld.types.GenericFormField
import realworld.types.InputType
import realworld.types.LoginCredential
import realworld.types.validation.GenericError
import utils.Utils.writerNTF

import scala.concurrent.ExecutionContext.Implicits.global
import realworld.AppState

final case class Login()(using api: Api, state: AppState) extends Component:
  val credential: Var[LoginCredential] = Var(LoginCredential(Email(""), Password("")))
  val emailWriter: Observer[String]    = credential.writerNTF(Email, _.focus(_.email).optic)
  val passwordWriter: Observer[String] = credential.writerNTF(Password, _.focus(_.password).optic)
  val errors: Var[GenericError]        = Var(Map())
  val handler = Observer[LoginCredential] { case LoginCredential(email, password) =>
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
  override def body: HtmlElement =
    div(
      cls := "auth-page",
      ContainerPage(
        div(
          cls := "col-md-6 offset-md-3 col-xs-12",
          h1(cls := "text-xs-center", "Sign In"),
          p(cls  := "text-xs-center", a(href := "/#/register", "Need an account?")),
          CredentialForm(
            errors.signal,
            onSubmit.preventDefault.mapTo(credential.now()) --> handler,
            "Sign in",
            state.s_login,
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
          ).fragement
        )
      )
    )
  end body
end Login
