package realworld.components.pages

import com.raquo.laminar.api.L.*
import monocle.syntax.all.*
import realworld.{authenticatedOnly, AppState, AuthEvent, AuthState}
import realworld.api.*
import realworld.components.Component
import realworld.components.widgets.{ContainerPage, GenericForm}
import realworld.routes.{JsRouter, Page}
import realworld.spec.{Bio, UnprocessableEntity, UpdateUserOutput}
import realworld.types.{FieldType, GenericFormField, InputType, Username, UserSettings}
import realworld.types.validation.GenericError
import utils.Utils.*

import scala.concurrent.ExecutionContext.Implicits.global
final case class Settings()(using state: AppState, api: Api) extends Component:
  val userSettings =
    Var(
      state.user
        .map(u => UserSettings(u.email.some, u.username.some, bio = u.bio, image = u.image))
        .getOrElse(UserSettings())
    )
  val emailWriter    = userSettings.writerOptF(_.focus(_.email).optic)
  val usernameWriter = userSettings.writerOptF(_.focus(_.username).optic)
  val bioWriter      = userSettings.writerOptNTF(Bio, _.focus(_.bio).optic)
  val imageWriter    = userSettings.writerOptF(_.focus(_.image).optic)
  val passwordWriter = userSettings.writerOptF(_.focus(_.password).optic)
  val handler = Observer[UserSettings] { us =>
    isUpdating.set(true)
    state.authHeader.fold(JsRouter.redirectTo(Page.Home))(authHeader =>
      us.validatedToReqData
        .foldError(api.userPromise.updateUser(authHeader, _).attempt)
        .collect {
          case Left(UnprocessableEntity(Some(e))) =>
            Var.set(
              isUpdating -> false,
              errors     -> e
            )
          case Left(e) =>
            Var.set(
              isUpdating -> false,
              errors     -> Map("error" -> List(e.getMessage()))
            )
          case Right(UpdateUserOutput(u)) =>
            state.events.emit(AuthEvent.Force(AuthState.Token(u.token.toAuthHeader, u)))
            JsRouter.redirectTo(Page.Home)
        }
    )
  }

  val logoutHandler = Observer[Unit] { _ =>
    state.events.emit(AuthEvent.Reset)
    JsRouter.redirectTo(Page.Home)
  }
  val errors: Var[GenericError] = Var(Map())
  val isUpdating                = Var(false)

  def body: HtmlElement =
    div(
      authenticatedOnly,
      cls := "settings-page",
      ContainerPage(
        div(
          cls := "col-md-6 offset-md-3 col-xs-12",
          h1(cls := "text-xs-center", "Your Settings"),
          GenericForm(
            errors.signal,
            onSubmit.preventDefault.mapTo(userSettings.now()) --> handler,
            "Update Settings",
            isUpdating.signal,
            List(
              GenericFormField(
                placeholder = "URL of profile picture",
                controlled = controlled(
                  value <-- userSettings.signal.map(_.image.getOrElse("")),
                  onInput.mapToValue --> imageWriter
                )
              ),
              GenericFormField(
                placeholder = "Your Name",
                controlled = controlled(
                  value <-- userSettings.signal.map(_.username.getOrElse("")),
                  onInput.mapToValue --> usernameWriter
                )
              ),
              GenericFormField(
                placeholder = "Short bio about you",
                fieldType = FieldType.Textarea,
                controlled = controlled(
                  value <-- userSettings.signal.map(_.bio.map(_.value).getOrElse("")),
                  onInput.mapToValue --> bioWriter
                ),
                rows = 8.some
              ),
              GenericFormField(
                placeholder = "Email",
                controlled = controlled(
                  value <-- userSettings.signal.map(_.email.getOrElse("")),
                  onInput.mapToValue --> emailWriter
                )
              ),
              GenericFormField(
                tpe = InputType.Password,
                placeholder = "Password",
                controlled = controlled(
                  value <-- userSettings.signal.map(_.password.getOrElse("")),
                  onInput.mapToValue --> passwordWriter
                )
              )
            )
          ).fragement,
          hr(),
          button(
            cls := "btn btn-outline-danger",
            "Or click here to logout.",
            onClick.mapToUnit --> logoutHandler
          )
        )
      )
    )
  end body
end Settings
