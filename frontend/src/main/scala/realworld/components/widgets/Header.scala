package realworld.components.widgets

import com.raquo.laminar.api.L.{*, given}
import realworld.routes.Page
import realworld.routes.JsRouter.*
import realworld.routes.JsRouter
import realworld.spec.Username
import realworld.AppState
import realworld.AuthState

def Header()(using state: AppState) =
  navTag(
    cls := "navbar navbar-light",
    div(
      cls := "container",
      a(cls("navbar-brand"), "conduit", navigateTo(Home)),
      ul(
        cls("nav navbar-nav pull-xs-right"),
        navItem("Home", Page.Home),
        children <-- state.s_auth.map {
          case Some(AuthState.Token(_, user)) => authenticatedLinks(user.username)
          case _                              => guestLinks
        }
      )
    )
  )

private def guestLinks =
  List(
    navItem("Sign in", Page.Login, "ion-compose"),
    navItem("Sign up", Page.Register, "ion-gear-a")
  )

private def authenticatedLinks(username: Username) =
  List(
    navItem("New Article", Page.NewArticle, "ion-compose"),
    navItem("Settings", Page.Setting, "ion-gear-a"),
    navItem(username.value, Page.ProfilePage(username))
  )

def navItem(text: String, page: Page, icon: String = "") =
  li(
    cls("nav-item"),
    a(
      cls("nav-link"),
      cls <-- JsRouter.currentPageSignal.map(cur => if cur == page then "active" else ""),
      navigateTo(page),
      if icon.nonEmpty then List[Modifier.Base](i(cls(icon)), s" $text") else text
    )
  )
