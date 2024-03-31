package realworld.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.modifiers.RenderableNode
import com.raquo.laminar.nodes.ChildNode.Base

trait Component:
  def body: HtmlElement           = fragement.head
  def fragement: Seq[HtmlElement] = Seq(body)

object Component:
  given RenderableNode[Component] with
    def asNode(value: Component): Base =
      value.body
    def asNodeIterable(values: Iterable[Component]): Iterable[Base] = values.flatMap(_.fragement)
    def asNodeOption(value: Option[Component]): Option[Base]        = value.map(_.body)
    def asNodeSeq(values: Seq[Component]): Seq[Base]                = values.flatMap(_.fragement)

  implicit def component2Elements(component: Component): Seq[HtmlElement] =
    component.fragement
