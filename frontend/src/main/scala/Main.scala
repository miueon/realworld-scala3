import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import integration.ShoelaceWebComponents
import realworld.components.widgets.Footer
import realworld.components.widgets.Header
import com.raquo.waypoint.Router
import realworld.routes.Page
import realworld.components.pages.Home
import realworld.api.Api
import todomvc.TodoMvcApp

object Main:
  def main(args: Array[String]): Unit =
    given Api = Api.create()
    renderOnDomContentLoaded(dom.document.querySelector("#app"),  TodoMvcApp.node)
