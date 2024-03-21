package realworld.routes

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L
import io.circe.*
import io.circe.syntax.*
import com.raquo.waypoint.*
import scala.scalajs.js.JSON
import urldsl.language.PathSegment
import urldsl.errors.DummyError

sealed trait Page derives Codec.AsObject
object Page:
  case object Home   extends Page
  case object SignIn extends Page
  case object SignUp extends Page

  def fragmentStatic[Page](staticPage: Page, pattern: PathSegment[Unit, DummyError]) =
    Route.static(staticPage, pattern, Route.fragmentBasePath)

  val homeRoute   = fragmentStatic(Page.Home, root / endOfSegments)
  val signInRoute = fragmentStatic(Page.SignIn, root / "login")
  val signUpRoute = fragmentStatic(Page.SignUp, root / "register")

  val router = new Router[Page](
    routes = List(homeRoute, signInRoute, signUpRoute),
    getPageTitle = { case _ =>
      "conduit"
    },
    serializePage = pg => pg.asJson.noSpaces,
    deserializePage = str =>
      io.circe.scalajs.decodeJs[Page](JSON.parse(str)).fold(throw _, identity)
  )(
    popStateEvents = windowEvents(_.onPopState),
    owner = L.unsafeWindowOwner
  )
end Page
