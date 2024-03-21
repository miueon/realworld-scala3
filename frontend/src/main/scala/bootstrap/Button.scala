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
    button(tpe := "button", cls := "btn btn-primary", idAttr := "liveToastBtn", "Show live toast"),
    div(
      cls       := "position-fixed bottom-0 right-0 p-3",
      styleAttr := "z-index: 5; right: 0; bottom: 0;",
      div(
        idAttr            := "liveToast",
        cls               := "toast hide",
        role              := "alert",
        aria.live         := "assertive",
        aria.atomic       := true,
        dataAttr("delay") := "2000",
        div(
          cls := "toast-header",
          img(src    := "...", cls := "rounded mr-2", alt := "..."),
          strong(cls := "mr-auto", "Bootstrap"),
          button(
            tpe                 := "button",
            cls                 := "ml-2 mb-1 close",
            dataAttr("dismiss") := "toast",
            aria.label          := "close",
            span(aria.hidden := true, "&times;")
          )
        ),
        div(cls := "toast-body", "Hello world! This is a toast message.")
      )
    ),
    div(
      cls       := "card",
      styleAttr := "width: 18rem",
      img(
        src := "...",
        cls := "card-img-top",
        alt := "..."
      ),
      div(
        cls := "card-body",
        h5(cls("card-title"), "Card title"),
        p(
          cls("card-text"),
          "Some quick example text to build on the card title and make up the bulk of the card's content."
        ),
        a(
          href := "#",
          cls  := "btn btn-primary",
          "Go somewhere"
        )
      )
    ),
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
