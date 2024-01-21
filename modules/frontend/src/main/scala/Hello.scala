import com.raquo.laminar.api.L.*
import org.scalajs.dom

def hello = 
  div(
    className := "text-10xl font-thicker font-fancy-sans backdrop:fi",
    span(
      "Hello, World"
    )
  )

@main
def helloWorld = 
  render(dom.document.querySelector("#app"), hello)