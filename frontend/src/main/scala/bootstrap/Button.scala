package bootstrap

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom

import cats.syntax.all.*
import com.raquo.laminar.api.L.{*, given}
import scala.scalajs.js.Date
def badge() =
  div(
    // button(
    //   tpe("button"),
    //   cls := "btn btn-primary",
    //   "Notifications",
    //   span(
    //     cls := "badge badge-light",
    //     "4"
    //   ),
    div(
      cls := "progress",
      div(
        cls("progress-bar w-75"),
        role("progressbar"),
        aria.valueNow(75),
        aria.valueMin(0),
        aria.valueMax(100)
      )
    ),
    navTag(
      cls := "navbar navbar-light",
      div(
        cls := "container",
        a(
          cls := "navbar-brand",
          "conduit"
        ),
        ul(
          cls := "nav navbar-nav pull-xs-right",
          li(
            cls := "nav-item",
            a(cls := "nav-link", "Home")
          ),
          li(
            cls := "nav-item",
            a(cls := "nav-link active", "Sign in")
          ),
          li(
            cls := "nav-item",
            a(cls := "nav-link", "Sign up")
          )
        )
      )
    )
  )
