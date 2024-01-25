import com.raquo.laminar.api.L.*
import org.scalajs.dom
import jobby.Api

def hello(using api: Api) =
  div(
    className := "font-medium",
    child.text <-- api.stream { a =>
      a.hello.get().map(_.message)
    }
  )

@main
def helloWorld =
  given Api = Api.create()
  render(dom.document.querySelector("#app"), hello)
