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

object Button extends WebComponent("sl-button"):
  self =>
  @JSImport("@shoelace-style/shoelace/dist/components/button/button.js", JSImport.Namespace)
  @js.native
  protected object RawImport extends js.Object

  type Ref = dom.HTMLButtonElement with HasGetForm

  // Events
  export L.onClick

  export CommonKeys.{onBlur, onFocus, onInvalid}

  // Props

  export CommonKeys.{size, variant}
  lazy val caret: HtmlPropOf[Boolean] = boolProp("caret")
  export L.{disabled, typ, tpe, nameAttr as name, value}
  export L.{formId, formAction, formEncType, formMethod, formNoValidate, formTarget}

  /** Draws the button in a loading state. */
  lazy val loading: HtmlPropOf[Boolean] = boolProp("loading")

  /** Draws an outlined button. */
  lazy val outline: HtmlPropOf[Boolean] = boolProp("outline")

  /** Draws a pill-style button with rounded edges. */
  lazy val pill: HtmlPropOf[Boolean] = boolProp("pill")

  /** Draws a circular icon button. When this attribute is present, the button expects a single
    * <sl-icon> in the default slot.
    */
  lazy val circle: HtmlPropOf[Boolean] = boolProp("circle")

  /** When set, the underlying button will be rendered as an <a> with this href instead of a
    * <button>. Warning: Note that in this case, our `Ref` type will be incorrect.
    */
  lazy val href: HtmlAttr[String] = stringAttr("href")

  /** Tells the browser where to open the link. Only used when href is present. */
  lazy val target: HtmlAttr[String] = stringAttr("target")

  /** When using href, this attribute will map to the underlying link’s rel attribute. Unlike
    * regular links, the default is noreferrer noopener to prevent security exploits. However, if
    * you’re using target to point to a specific tab/window, this will prevent that from working
    * correctly. You can remove or change the default value by setting the attribute to an empty
    * string or a value of your choice, respectively.
    */
  lazy val rel: HtmlAttr[String] = stringAttr("rel")

  /** Tells the browser to download the linked file as this filename. Only used when href is
    * present.
    */
  lazy val download: HtmlAttr[String] = stringAttr("download")

  object slots:
    lazy val prefix = Slot("prefix")
    lazy val suffix = Slot("suffix")

  object parts:

    /** The component’s base wrapper. */
    val base: String = "base"

    /** The container that wraps the prefix. */
    val prefix: String = "prefix"

    /** The button’s label. */
    val label: String = "label"

    /** The container that wraps the suffix. */
    val suffix: String = "suffix"

    /** The button’s caret icon, an <sl-icon> element. */
    val caret: String = "caret"

    /** The spinner that shows when the button is in the loading state. */
    val spinner: String = "spinner"
  end parts
end Button
