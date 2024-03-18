package vendor.shoelace.components

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.defs.styles.{traits as s}
import com.raquo.laminar.defs.styles.{units as u}
import org.scalajs.dom
import vendor.shoelace.CommonKeys
import vendor.shoelace.HasGetForm
import vendor.shoelace.Slot
import vendor.shoelace.WebComponent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Card extends WebComponent("sl-card"):
  self =>
  @JSImport("@shoelace-style/shoelace/dist/components/card/card.js", JSImport.Namespace)
  @js.native
  protected object RawImport extends js.Object

  // Props
  lazy val padding = ""

  object slots:
    lazy val header = Slot("header")
    lazy val footer = Slot("footer")
    lazy val image  = Slot("image")

  object parts:
    val base   = "base"
    val image  = "image"
    val header = "header"
    val body   = "body"
    val footer = "footer"
end Card
