package realworld.components.pages

import com.raquo.laminar.api.L.*
import monocle.syntax.all.*
import realworld.AppState
import realworld.components.Component
import realworld.components.widgets.ContainerPage
import realworld.components.widgets.GenericForm
import realworld.spec.Bio
import realworld.spec.Email
import realworld.spec.ImageUrl
import realworld.spec.Username
import realworld.types.UserSettings
import realworld.types.validation.GenericError
import utils.Utils.some
import utils.Utils.writerOptNTF
import realworld.types.GenericFormField
import realworld.types.InputType
import realworld.types.FieldType
import realworld.AuthEvent
import realworld.routes.JsRouter
import realworld.routes.Page
import realworld.api.Api
import realworld.spec.UpdateUserData
import realworld.spec.UnprocessableEntity
import realworld.spec.UpdateUserOutput

import scala.concurrent.ExecutionContext.Implicits.global
import realworld.authenticatedOnly
import realworld.AuthState
import utils.Utils.toAuthHeader
final case class Settings()(using state: AppState, api: Api) extends Component:
  val userSettings =
    Var(
      state.user
        .map(u => UserSettings(u.email.some, u.username.some, bio = u.bio, image = u.image))
        .getOrElse(UserSettings())
    )
  val emailWriter    = userSettings.writerOptNTF(Email, _.focus(_.email).optic)
  val usernameWriter = userSettings.writerOptNTF(Username, _.focus(_.username).optic)
  val bioWriter      = userSettings.writerOptNTF(Bio, _.focus(_.bio).optic)
  val imageWriter    = userSettings.writerOptNTF(ImageUrl, _.focus(_.image).optic)
  val passwordWriter = userSettings.writerOptNTF(Email, _.focus(_.email).optic)
  val handler = Observer[UserSettings]:
    case UserSettings(email, username, password, bio, image) =>
      isUpdating.set(true)
      state.authHeader.fold(JsRouter.redirectTo(Page.Home))(authHeader =>
        api
          .future(
            _.users
              .updateUser(authHeader, UpdateUserData(email, username, password, bio, image))
              .attempt
          )
          .onComplete(_ => isUpdating.set(false))
          .collect {
            case Left(UnprocessableEntity(Some(e))) => errors.set(e)
            case Right(UpdateUserOutput(u)) =>
              state.events.emit(AuthEvent.Force(AuthState.Token(u.token.toAuthHeader, u)))
              JsRouter.redirectTo(Page.Home)
          }
      )

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
                  value <-- userSettings.signal.map(_.image.map(_.value).getOrElse("")),
                  onInput.mapToValue --> imageWriter
                )
              ),
              GenericFormField(
                placeholder = "Your Name",
                controlled = controlled(
                  value <-- userSettings.signal.map(_.username.map(_.value).getOrElse("")),
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
                  value <-- userSettings.signal.map(_.email.map(_.value).getOrElse("")),
                  onInput.mapToValue --> emailWriter
                )
              ),
              GenericFormField(
                tpe = InputType.Password,
                placeholder = "Password",
                controlled = controlled(
                  value <-- userSettings.signal.map(_.password.map(_.value).getOrElse("")),
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
