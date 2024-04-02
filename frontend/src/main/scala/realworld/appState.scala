package realworld

import com.raquo.laminar.api.L.*
import realworld.spec.AuthHeader
import realworld.spec.User

enum AuthState:
  case Unauthenticated
  case Token(value: AuthHeader, user: User)

enum AuthEvent:
  case Force(value: AuthState)
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

  val s_authHeader = _authToken.signal.map {
    case Some(tok: AuthState.Token) => Some(tok.value)
    case _                          => None
  }

  def authHeader = _authToken.now() match
    case Some(tok: AuthState.Token) => Some(tok.value)
    case _                          => None

  val tokenWriter = _authToken.writer
end AppState

// def test = 
//   dom.window.localStorage.getItem("test")

object AppState:
  def init =
    AppState(
      _authToken = Var(None),
      events = EventBus[AuthEvent]()
    )
end AppState
