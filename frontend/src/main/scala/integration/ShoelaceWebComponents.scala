package integration

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import utils.Utils.useImport
import vendor.shoelace.components.Button
import vendor.shoelace.components.Icon

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import vendor.shoelace.Shoelace
import vendor.shoelace.components.Breadcrumb
import vendor.shoelace.components.BreadcrumbItem
import vendor.shoelace.components.Card
import vendor.shoelace.components.Rating

object ShoelaceWebComponents:
  @JSImport("@find/**/ShoelaceView.less?inline", JSImport.Namespace)
  @js.native
  private object Stylesheet extends js.Object

  useImport(Stylesheet)

  // Load Shoelace themes. Light one is the default, but we make a button to switch them.
  // See their contents at https://github.com/shoelace-style/shoelace/blob/current/src/themes/light.css
  // BEGIN[shoelace/themes]
  @JSImport("@shoelace-style/shoelace/dist/themes/light.css", "importStyle")
  @js.native
  private def importLightTheme(): Unit = js.native

  importLightTheme()

  // This path is determined by `dest` config of `rollupCopyPlugin` in vite.config.js
  // Note: This needs to be called once, prior to loading any Shoelace components
  //   JsApp would be a good place to put this, I'm just putting it here because
  //   in this demo project, this is the only view that uses Shoelace components.
  Shoelace.setBasePath("/assets/shoelace")
  def apply() =
    div(
      cls("ShoelaceWebComponentsView"),
      p(
        Button(
          _ => "Reload",
          _ => onClick --> { _ => dom.window.alert("Clicked") },
          _.slots.prefix(
            Icon.of(_.name("arrow-counterclockwise"))
          )
        ),
        " ",
        Button(
          _.variant.warning,
          _ => "User",
          // _ => onClick --> { _ => dom.window.alert("Clicked") },
          onClick --> { _ => dom.window.alert("ClickedUser") },
          _.slots.suffix(
            Icon.of(_.name("person-fill"))
          )
        )
      ),
      Breadcrumb(
        BreadcrumbItem(
          _.slots.prefix(
            Icon(_.name("house"))
          ),
          _ => "Home"
        ),
        BreadcrumbItem(_ => "Clothing"),
        BreadcrumbItem(_ => "Shirts")
      ),
      Card(
        cls := "card-overview",
        img(
          slot := "image",
          src := "https://images.unsplash.com/photo-1559209172-0ff8f6d49ff7?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=500&q=80",
          alt := "A kitten sits patiently between a terracotta pot and decorative grasses."
        ),
        strong("Mittens"),
        "This kitten is as cute as he is playful. Bring him home today!",
        br(),
        small("6 weeks old"),
        div(
          slot := "footer",
          Button(_.variant("primary"), _.pill := true),
          Rating(
            cls := "rating-emojis",
            _.getSymbol(v =>
              val icons = Array(
                "emoji-angry",
                "emoji-frown",
                "emoji-expressionless",
                "emoji-smile",
                "emoji-laughing"
              )
              s"<sl-icon name='${icons(v - 1)}'></sl-icon>"
            )
          )
        )
      )
    )
end ShoelaceWebComponents
