package realworld.components.widgets

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

def ContainerPage(content: Modifier[ReactiveHtmlElement[dom.html.Element]]*): Div =
  div(
    cls := "container page",
    div(
      cls := "row",
      content
    )
  )
