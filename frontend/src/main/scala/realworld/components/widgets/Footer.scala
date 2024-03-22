package realworld.components.widgets

import com.raquo.laminar.api.L.{*, given}


def Footer() =
  footerTag(
    div(
      cls := "container",
      a(href := "/#/", cls := "logo-font", "conduit"),
      span(
        cls := "attribution",
        "An interactive learning project from ",
        a(href := "https://thinkster.io", "Thinkster"),
        ". Code & design licensed under MIT."
      )
    )
  )
