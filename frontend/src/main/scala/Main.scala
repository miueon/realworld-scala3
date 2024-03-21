import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import integration.ShoelaceWebComponents
import realworld.components.footer.Footer
import realworld.components.header.Header
import com.raquo.waypoint.Router
import realworld.routes.Page

object Main:
  def main(args: Array[String]): Unit =
    given Router[Page] = Page.router
    // Laminar initialization
    renderOnDomContentLoaded(dom.document.querySelector("#app"), Header())
