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
object Breadcrumb extends WebComponent("sl-breadcrumb"):
  self =>
  @JSImport("@shoelace-style/shoelace/dist/components/breadcrumb/breadcrumb.js", JSImport.Namespace)
  @js.native
  protected object RawImport extends js.Object

  // type Ref = dom.HTMLElement

  // Events

  // Props

  lazy val label: HtmlAttr[String] = stringAttr("label")

  object slots:
    lazy val separator = Slot("separator")

  object parts:
    val base = "base"
end Breadcrumb
