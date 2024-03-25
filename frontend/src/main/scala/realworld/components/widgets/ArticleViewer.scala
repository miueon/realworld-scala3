package realworld.components.widgets

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import realworld.spec.TagName
import realworld.components.Component

final case class TagList(tagList: List[TagName]) extends Component:
  def body: HtmlElement =
    ul(
      cls := "tag-list",
      tagList.map(tag =>
        li(
          cls := "tag-default tag-pill tag-outline",
          tag.value
        )
      )
    )
