package realworld


import com.raquo.waypoint.*
import urldsl.language.PathSegment
import urldsl.errors.DummyError
package object routes:
  def fragmentStatic[Page](staticPage: Page, pattern: PathSegment[Unit, DummyError]) =
    Route.static(staticPage, pattern, Route.fragmentBasePath)

  val routes = List(
    fragmentStatic(Page.Home, root / endOfSegments),
    fragmentStatic(Page.SignIn, root / "login"),
    fragmentStatic(Page.SignUp, root / "register")
  )
end routes
