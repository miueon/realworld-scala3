package vendor.shoelace.components

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.defs.styles.{traits as s}
import com.raquo.laminar.defs.styles.{units as u}
import org.scalajs.dom
import vendor.shoelace.CommonKeys
import vendor.shoelace.HasGetForm
import vendor.shoelace.WebComponent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Button extends WebComponent("sl-button"):
  self =>
  @JSImport("@shoelace-style/shoelace/dist/components/button/button.js", JSImport.Namespace)
  @js.native
  protected object RawImport extends js.Object

  type Ref = dom.HTMLInputElement with HasGetForm

  export CommonKeys.{onChange, onInput, onBlur, onFocus, onInvalid}

  export L.{nameAttr as name, value, disabled, required, checked, defaultChecked, formId}

  @inline def noSlots: Unit = ()

  lazy val width: StyleProp[String] with s.Auto with u.Length[DSP, Int] = lengthAutoStyle("--width")

  lazy val height: StyleProp[String] with s.Auto with u.Length[DSP, Int] = lengthAutoStyle(
    "--height"
  )

  lazy val thumbSize: StyleProp[String] with s.Auto with u.Length[DSP, Int] = lengthAutoStyle(
    "--thumb-size"
  )

  object parts:
    val base    = "base"
    val control = "control"
    val thumb   = "thumb"
    val label   = "label"
end Button
