package realworld

import com.raquo.waypoint.Router
import com.raquo.laminar.api.L.*
import scala.util.Try
import scala.util.Success
import scala.util.Failure

package object routes:

  def redirectTo(pg: Page)(using router: Router[Page]) =
    router.pushState(pg)
  def navigateTo(page: Page)(using router: Router[Page]): Binder[HtmlElement] =
    Binder { el =>
      import org.scalajs.dom
      val isLinkElement = el.ref.isInstanceOf[dom.html.Anchor]
      if isLinkElement then
        // el.amend(router.absoluteUrlForPage(page))
        Try(router.absoluteUrlForPage(page)) match
          case Success(url)       => el.amend(href(url))
          case Failure(exception) => dom.console.error(exception)

      (onClick
        .filter(ev => !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey)))
        .preventDefault --> (_ => redirectTo(page))).bind(el)
    }
end routes
