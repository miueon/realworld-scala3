package realworld.components

import com.raquo.laminar.api.L.{*, given}

trait Component:
  def body: HtmlElement

object Component:
  implicit def component2HtmlElement(component: Component): HtmlElement =
    component.body
