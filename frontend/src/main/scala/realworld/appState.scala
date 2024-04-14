package realworld

import cats.effect.*
import com.raquo.laminar.api.L.*
import org.scalajs.dom
import realworld.api.Api
import realworld.spec.AuthHeader
import realworld.spec.Token
import realworld.spec.User
import utils.Utils.some

enum AuthState:
  case Unauthenticated
  case Token(value: AuthHeader, user: User)

enum AuthEvent:
  case Load
  case Force(value: AuthState.Token)
  case Reset

class AppState private (
    _authToken: Var[Option[AuthState]],
    val events: EventBus[AuthEvent]
):
  val s_token = _authToken.signal
  val s_login = s_token.map {
    case Some(tok: AuthState.Token) => true
    case _                          => false
  }

  val s_auth = _authToken.signal.map {
    case None | Some(AuthState.Unauthenticated) => None
    case t                                      => t
  }

  def authHeader = _authToken.now() match
    case Some(tok: AuthState.Token) => Some(tok.value)
    case _                          => None

  def user = _authToken.now() match
    case Some(AuthState.Token(_, user)) => Some(user)
    case _                              => None

  val tokenWriter = _authToken.writer
end AppState

class AuthStateWatcher(bus: EventBus[AuthEvent])(using state: AppState, api: Api):
  def loop =
    eventSources
      .withCurrentValueOf(state.s_token)
      .flatMap {
        case (AuthEvent.Load, None) =>
          val header = loadAuthHeader
          api.stream(_.users.getUser(header).attempt.flatMap {
            case Left(_)      => IO.pure(AuthState.Unauthenticated.some)
            case Right(value) => IO.pure(AuthState.Token(header, value.user).some)
          })
        case (AuthEvent.Load, s) => EventStream.fromValue(s)
        case (AuthEvent.Force(s), _) =>
          saveToken(s.user.token.getOrElse(Token("")))
          EventStream.fromValue(s.some)
        case (AuthEvent.Reset, _) =>
          clearToken
          EventStream.fromValue(AuthState.Unauthenticated.some)
      } --> state.tokenWriter

  private def loadAuthHeader =
    val token = dom.window.localStorage.getItem("token")
    AuthHeader(s"Token $token")

  private def saveToken(h: Token) =
    dom.window.localStorage.setItem("token", h.value)

  private def clearToken = dom.window.localStorage.removeItem("token")

  private val eventSources =
    EventStream.merge(bus.events, state.s_token.changes.collect { case None => AuthEvent.Reset })
end AuthStateWatcher

object AppState:
  def init =
    AppState(
      _authToken = Var(None),
      events = EventBus[AuthEvent]()
    )
end AppState
