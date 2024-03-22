package realworld.components.pages
import com.raquo.laminar.api.L.{*, given}
import realworld.components.widgets.ContainerPage

object Home:
  def apply() =
    div(
      cls := "home-page",
      banner(),
      ContainerPage(
        div(cls := "col-md-9", "test")
      )
    )

  def banner() =
    div(
      cls := "banner",
      div(
        cls := "container",
        h1(cls := "logo-font", "conduit"),
        p("A place to share your knowledge.")
      )
    )
end Home
