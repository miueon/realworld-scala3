package realworld.components.pages

import com.raquo.laminar.api.L.*
import monocle.syntax.all.*
import realworld.api.*
import realworld.components.Component
import realworld.components.widgets.{ContainerPage, GenericForm}
import realworld.routes.JsRouter.*
import realworld.routes.Page
import realworld.spec.{RegisterUserOutput, UnprocessableEntity}
import realworld.types.validation.GenericError
import realworld.types.{GenericFormField, InputType, RegisterCredential}
import realworld.{AppState, AuthEvent, AuthState, guestOnly}
import utils.Utils.*

import scala.concurrent.ExecutionContext.Implicits.global
final case class Register()(using api: Api, state: AppState) extends Component:
  val credential                = Var(RegisterCredential())
  val usernameWriter            = credential.writerF(_.focus(_.username).optic)
  val emailWriter               = credential.writerF(_.focus(_.email).optic)
  val passwordWriter            = credential.writerF(_.focus(_.password).optic)
  val signingUp                 = Var(false)
  val errors: Var[GenericError] = Var(Map())
  val handler = Observer[RegisterCredential] { rc =>
    signingUp.set(true)
    rc.validatedToReqData
      .foldError(api.userPromise.registerUser(_).attempt)
      .collect {
        case Left(UnprocessableEntity(Some(e))) =>
          signingUp.set(false)
          errors.set(e)
        case Left(e) =>
          signingUp.set(false)
          errors.set(Map("error" -> List(e.getMessage())))
        case Right(RegisterUserOutput(usr)) =>
          errors.set(Map())
          state.events.emit(
            AuthEvent.Force(
              AuthState.Token(
                usr.token.toAuthHeader,
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
                InputType.Text,
                "Username",
                controlled = controlled(
                  value <-- credential.signal.map(_.username),
                  onInput.mapToValue --> usernameWriter
                )
              ),
              GenericFormField(
                InputType.Text,
                "Email",
                controlled = controlled(
                  value <-- credential.signal.map(_.email),
                  onInput.mapToValue --> emailWriter
                )
              ),
              GenericFormField(
                InputType.Password,
                "Password",
                controlled = controlled(
                  value <-- credential.signal.map(_.password),
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
