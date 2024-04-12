package realworld.components.pages

import com.raquo.laminar.api.L.*
import monocle.syntax.all.*
import realworld.AppState
import realworld.AuthEvent
import realworld.AuthState
import realworld.api.Api
import realworld.components.Component
import realworld.components.widgets.ContainerPage
import realworld.components.widgets.GenericForm
import realworld.routes.JsRouter.*
import realworld.routes.Page
import realworld.spec.AuthHeader
import realworld.spec.Email
import realworld.spec.Password
import realworld.spec.RegisterUserData
import realworld.spec.RegisterUserOutput
import realworld.spec.UnprocessableEntity
import realworld.spec.Username
import realworld.types.GenericFormField
import realworld.types.InputType
import realworld.types.RegisterCredential
import realworld.types.validation.GenericError
import utils.Utils.writerNTF

import scala.concurrent.ExecutionContext.Implicits.global
import realworld.guestOnly
final case class Register()(using api: Api, state: AppState) extends Component:
  val credential                = Var(RegisterCredential(Username(""), Email(""), Password("")))
  val usernameWriter            = credential.writerNTF(Username, _.focus(_.username).optic)
  val emailWriter               = credential.writerNTF(Email, _.focus(_.email).optic)
  val passwordWriter            = credential.writerNTF(Password, _.focus(_.password).optic)
  val signingUp                 = Var(false)
  val errors: Var[GenericError] = Var(Map())
  val handler = Observer[RegisterCredential] { case RegisterCredential(username, email, password) =>
    signingUp.set(true)
    api
      .future(
        _.users
          .registerUser(
            RegisterUserData(
              username,
              email,
              password
            )
          )
          .attempt
      )
      .collect {
        case Left(UnprocessableEntity(Some(e))) =>
          signingUp.set(false)
          errors.set(e)
        case Right(RegisterUserOutput(usr)) =>
          errors.set(Map())
          state.events.emit(
            AuthEvent.Force(
              AuthState.Token(
                AuthHeader(s"Token ${usr.token.get}"),
                usr
              )
            )
          )
          redirectTo(Page.Login)
      }
  }

  def body: HtmlElement =
    div(
      guestOnly,
      cls := "auth-page",
      ContainerPage(
        div(
          cls := "col-md-6 offset-md-3 col-xs-12",
          h1(cls := "text-xs-center", "Sign up"),
          p(cls  := "text-xs-center", a(navigateTo(Page.Login), "Have an account?")),
          GenericForm(
            errors.signal,
            onSubmit.preventDefault.mapTo(credential.now()) --> handler,
            "Sign up",
            signingUp.signal,
            List(
              GenericFormField(
                "Username",
                InputType.Text,
                "Username",
                controlled = controlled(
                  value <-- credential.signal.map(_.username.value),
                  onInput.mapToValue --> usernameWriter
                )
              ),
              GenericFormField(
                "Email",
                InputType.Text,
                "Email",
                controlled = controlled(
                  value <-- credential.signal.map(_.email.value),
                  onInput.mapToValue --> emailWriter
                )
              ),
              GenericFormField(
                "Password",
                InputType.Password,
                "Password",
                controlled = controlled(
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
end Register
