package realworld.routes

import com.raquo.laminar.api.L.*
import com.raquo.waypoint
import com.raquo.waypoint.*
import org.scalajs.dom
import upickle.default.*

import scala.util.{Failure, Success, Try}
object JsRouter
extends waypoint.Router[Page](
  routes = routes,
  getPageTitle = {
    case Page.Home                    => "Conduit"
    case Page.Login                   => "Conduit: Login"
    case Page.Register                => "Conduit: Register"
    case Page.Setting                 => "Conduit: Settings"
    case Page.NewArticle              => "Conduit: New Article"
    case Page.ArticleDetailPage(slug) => s"Conduit:"
    case Page.ProfilePage(username)   => s"Conduit: $username"
    case Page.EditArticlePage(slug)   => s"Conduit: Edit"
  },
  serializePage = pg => write(pg),
  deserializePage = str => read[Page](str)
)(popStateEvents = windowEvents(_.onPopState), owner = unsafeWindowOwner):
  export Page.*

  def navigateTo(page: Page): Binder[HtmlElement] = Binder { el =>
    val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]

    if isLinkElement then
      Try(absoluteUrlForPage(page)) match
        case Success(url) => el.amend(href(url))
        case Failure(err) => dom.console.error(s"$page $err")

    // If element is a link and user is holding a modifier while clicking:
    //  - Do nothing, browser will open the URL in new tab / window / etc. depending on the modifier key
    // Otherwise:
    //  - Perform regular pushState transition
    //  - Scroll to top of page

    val onRegularClick = onClick
      .filter(ev => !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey)))
      .preventDefault

    (onRegularClick --> { _ =>
      pushState(page)
      dom.window.scrollTo(0, 0) // Scroll to top of page when navigating
    }).bind(el)
  }

  def redirectTo(page: Page) =
    pushState(page)

  // Add this to h1..h6 to lmake title clickable, id would appear in the URL
  def titleLink(id: String, caption: String = "#"): Modifier.Base =
    List[Modifier.Base](
      Modifier(parentEl => parentEl.ref.id = id),
      // TODO update the cls
      a(cls("nav-link"), href(s"#$id"), caption)
    )
end JsRouter
