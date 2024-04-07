package realworld

import com.raquo.waypoint.*
import urldsl.language.PathSegment
import urldsl.errors.DummyError
import realworld.routes.Page.ProfilePage
import realworld.spec.Username
package object routes:
  def fragmentStatic[Page](staticPage: Page, pattern: PathSegment[Unit, DummyError]) =
    Route.static(staticPage, pattern, Route.fragmentBasePath)

  val profileRoute = Route(
    encode = (stp: ProfilePage) => stp.username.value,
    decode = (arg: String) => ProfilePage(Username(arg)),
    pattern = root / "profile" / segment[String] / endOfSegments,
    Route.fragmentBasePath
  )

  val routes = List(
    fragmentStatic(Page.Home, root / endOfSegments),
    fragmentStatic(Page.Login, root / "login"),
    fragmentStatic(Page.Register, root / "register"),

  )
end routes
