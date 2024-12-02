package realworld

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import realworld.api.*
import realworld.spec.{AuthHeader, Token, User}
import utils.Utils.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
  val tokenSignal = _authToken.signal
  val loginSignal = tokenSignal.map {
    case Some(tok: AuthState.Token) => true
    case _                          => false
  }

  val authSignal = _authToken.signal.map {
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

  val titleWriter = Observer[String] { title =>
    dom.document.title = title
  }
end AppState

class AuthStateWatcher(bus: EventBus[AuthEvent])(using state: AppState, api: Api):
  def loop =
    eventSources
      .withCurrentValueOf(state.tokenSignal)
      .flatMap {
        case (AuthEvent.Load, None) =>
          val header = loadAuthHeader
          api.promiseStream(_.userPromise.getUser(header).attempt.flatMap {
            case Left(_)      => Future.successful(AuthState.Unauthenticated.some)
            case Right(value) => Future.successful(AuthState.Token(header, value.user).some)
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
    EventStream.merge(
      bus.events,
      state.tokenSignal.changes.collect { case None => AuthEvent.Reset }
    )
end AuthStateWatcher

object AppState:
  def init =
    AppState(
      _authToken = Var(None),
      events = EventBus[AuthEvent]()
    )
end AppState
