package integration

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import utils.Utils.useImport
import vendor.shoelace.components.Button
import vendor.shoelace.components.Icon

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import vendor.shoelace.Shoelace

object ShoelaceWebComponents:
  @JSImport("@find/**/ShoelaceView.less?inline", JSImport.Namespace)
  @js.native
  private object Stylesheet extends js.Object


  useImport(Stylesheet)

  // Load Shoelace themes. Light one is the default, but we make a button to switch them.
  // See their contents at https://github.com/shoelace-style/shoelace/blob/current/src/themes/light.css
  // BEGIN[shoelace/themes]
  @JSImport("@shoelace-style/shoelace/dist/themes/light.css", "importStyle")
  @js.native private def importLightTheme(): Unit = js.native

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
        Button.of(
          _ => "Reload",
          _ => onClick --> { _ => dom.window.alert("Clicked") },
          _.slots.prefix(
            Icon.of(_.name("arrow-counterclockwise"))
          )
        ),
        " ",
        Button.of(
          _.variant.success,
          _ => "User",
          _ => onClick --> { _ => dom.window.alert("Clicked") },
          _.slots.suffix(
            Icon.of(_.name("person-fill"))
          )
        )
      )
    )
end ShoelaceWebComponents
