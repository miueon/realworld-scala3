package realworld.components.pages

import com.raquo.laminar.api.L.*
import monocle.syntax.all.*
import realworld.{guestOnly, AppState, AuthEvent, AuthState}
import realworld.api.Api
import realworld.components.Component
import realworld.components.widgets.{ContainerPage, GenericForm}
import realworld.routes.JsRouter.*
import realworld.routes.Page
import realworld.spec.{LoginUserOutput, UnprocessableEntity}
import realworld.types.{FieldType, GenericFormField, InputType, LoginCredential}
import realworld.types.validation.GenericError
import utils.Utils.{attempt, foldError, toAuthHeader, writerF}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Thenable.Implicits.thenable2future

final case class Login()(using api: Api, state: AppState) extends Component:
  val credential: Var[LoginCredential] = Var(LoginCredential())
  val emailWriter: Observer[String]    = credential.writerF(_.focus(_.email).optic)
  val passwordWriter: Observer[String] = credential.writerF(_.focus(_.password).optic)
  val errors: Var[GenericError]        = Var(Map())
  val handler = Observer[LoginCredential] { it =>
    it.validatedToReqData
      .foldError(api.userPromise.loginUser(_).attempt)
      .collect {
        case Left(UnprocessableEntity(Some(e))) => errors.set(e)
        case Left(e)                            => errors.set(Map("error" -> List(e.getMessage())))
        case Right(LoginUserOutput(usr)) =>
          errors.set(Map())
          state.events.emit(
            AuthEvent.Force(
              AuthState.Token(
                usr.token.toAuthHeader,
                usr
              )
            )
          )
          redirectTo(Page.Home)
      }
  }
  override def body: HtmlElement =
    div(
      guestOnly,
      cls := "auth-page",
      ContainerPage(
        div(
          cls := "col-md-6 offset-md-3 col-xs-12",
          h1(cls := "text-xs-center", "Sign In"),
          p(cls  := "text-xs-center", a(navigateTo(Page.Register), "Need an account?")),
          GenericForm(
            errors.signal,
            onSubmit.preventDefault.mapTo(credential.now()) --> handler,
            "Sign in",
            state.loginSignal,
            List(
              GenericFormField(
                placeholder = "Email",
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
end Login
