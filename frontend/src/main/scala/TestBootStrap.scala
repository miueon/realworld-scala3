import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import typings.bootstrap.mod.*
import com.raquo.app.form.ControlledInputsView
object Main:
  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.querySelector("#app"), appElement())

  def appElement() =
    div(
      className := "card",
      styleAttr := "width: 18rem;",
      img(
        src := "https://via.placeholder.com/150",
        className := "card-img-top",
        alt := "..."
      ),
      div(
        className := "card-body",
        h5(className := "card-title", "Card title"),
        p(className := "card-text", "Some quick example text to build on the card title and make up the bulk of the card's content."),
        a(
          href := "#",
          className := "btn btn-primary",
          "Go somewhere")
      ),
      h1("Hello")
    )
