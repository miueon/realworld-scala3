package realworld.components.header

import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import realworld.routes.Page
import realworld.routes.navigateTo
import realworld.routes.Page.Home
import realworld.routes.Page.router

def Header()(using Router[Page]) =
  navTag(
    cls := "navbar navbar-light",
    div(
      cls := "container",
      a(cls("navbar-brand"), "conduit", navigateTo(Home)),
      ul(
        cls("nav navbar-nav pull-xs-right"),
        navItem("Home", Home),
        navItem("Sign in", Page.SignIn, "ion-compose"),
        navItem("Sign up", Page.SignUp, "ion-gear-a")
      )
    )
  )

def navItem(text: String, page: Page, icon: String = "")(using Router[Page]) =
  li(
    cls("nav-item"),
    a(
      cls("nav-link"),
      cls <-- router.currentPageSignal.map(cur => if cur == page then "active" else ""),
      navigateTo(page),
      if icon.nonEmpty then List[Modifier.Base](i(cls(icon)), s" $text") else text
    )
  )
