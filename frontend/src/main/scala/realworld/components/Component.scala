package realworld.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.modifiers.RenderableNode
import com.raquo.laminar.nodes.ChildNode.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom

trait Component:
  def body: HtmlElement

trait ComponentSeq:
  def fragement: Seq[Modifier[ReactiveHtmlElement[dom.html.Element]]]

object Component:
  given RenderableNode[Component] with
    def asNode(value: Component): Base =
      value.body
    def asNodeIterable(values: Iterable[Component]): Iterable[Base] = values.map(_.body)
    def asNodeOption(value: Option[Component]): Option[Base]        = value.map(_.body)
    def asNodeSeq(values: Seq[Component]): Seq[Base]                = values.map(_.body)
