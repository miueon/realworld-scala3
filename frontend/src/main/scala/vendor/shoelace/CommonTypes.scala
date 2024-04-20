package vendor.shoelace

import com.raquo.laminar.api.L.*
import com.raquo.laminar.codecs.*
import com.raquo.laminar.defs.styles.traits as s
import com.raquo.laminar.defs.styles.units as u
import com.raquo.laminar.keys
import com.raquo.laminar.keys.DerivedStyleProp
import com.raquo.laminar.modifiers.KeySetter
import com.raquo.laminar.modifiers.KeySetter.StyleSetter

import scala.scalajs.js

trait CommonTypes:

  // #TODO Move to Laminar
  type HtmlPropOf[V] = keys.HtmlProp[V, V]

  // #TODO Move to Laminar
  type PropSetterOf[A] = KeySetter.PropSetter[A, A]

  // # TODO Use Laminar alias
  protected type DSP[V] = DerivedStyleProp[V]

  // # TODO Use Laminar alias
  protected type SS = StyleSetter

  // #TODO I should make use of Laminar helpers like lengthAutoStyle in StyleProps.scala,
  //  but they're defined together with the listings in the same traits, and I don't want
  //  to expose all those props Split them out. Need minor breaking changes in Laminar.

  protected def stringProp(name: String): HtmlPropOf[String] = htmlProp(name, StringAsIsCodec)

  protected def boolProp(name: String): HtmlPropOf[Boolean] = htmlProp(name, BooleanAsIsCodec)

  protected def boolAttr(name: String): HtmlAttr[Boolean] =
    htmlAttr(name, BooleanAsAttrPresenceCodec)

  protected def stringAttr(name: String): HtmlAttr[String] = htmlAttr(name, StringAsIsCodec)

  protected def doubleProp(name: String) = htmlProp(name, DoubleAsIsCodec)
  protected def doubleAttr(name: String) = htmlAttr(name, DoubleAsStringCodec)

  protected def callBack(name: String)(value: scala.scalajs.js.Any) =
    inContext(thisNode =>
      onMountCallback(_ => thisNode.ref.asInstanceOf[js.Dynamic].updateDynamic(name)(value))
    )

  // protected def callBack(name: String) =

  protected def lengthAutoStyle(
      name: String
  ): StyleProp[String] & s.Auto & u.Length[DSP, Int] =
    new StyleProp[String](name) with s.Auto with u.Length[DSP, Int]
end CommonTypes
