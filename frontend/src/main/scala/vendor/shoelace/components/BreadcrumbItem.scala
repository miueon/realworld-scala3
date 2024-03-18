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

object BreadcrumbItem extends WebComponent("sl-breadcrumb-item"):
  self =>
  @JSImport(
    "@shoelace-style/shoelace/dist/components/breadcrumb-item/breadcrumb-item.js",
    JSImport.Namespace
  )
  @js.native
  protected object RawImport extends js.Object

  // Props

  lazy val href = stringAttr("href")

  lazy val target = stringAttr("target")

  lazy val ref = stringAttr("rel")

  object slots:
    lazy val prefix = Slot("prefix")
    lazy val suffix = Slot("suffix")
    lazy val separator = Slot("separator")

  object parts:

    /** The component’s base wrapper. */
    val base: String = "base"

    /** The container that wraps the prefix. */
    val prefix: String = "prefix"

    /** The button’s label. */
    val label: String = "label"

    /** The container that wraps the suffix. */
    val suffix: String = "suffix"

    val separator = "separator"

  end parts

  
