import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import integration.ShoelaceWebComponents
import realworld.components.widgets.Footer
import realworld.components.widgets.Header
import com.raquo.waypoint.Router
import realworld.routes.Page
import realworld.components.pages.Home

object Main:
  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.querySelector("#app"), Home())
