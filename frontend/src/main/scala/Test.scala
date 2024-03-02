import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import todomvc.TodoMvcApp
import integration.ShoelaceWebComponents

object Main {
  def main(args: Array[String]): Unit = {
    // Laminar initialization
    renderOnDomContentLoaded(dom.document.querySelector("#app"), ShoelaceWebComponents.apply())
  }
}