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

object Rating extends WebComponent("sl-rating"):
  self =>
  @JSImport("@shoelace-style/shoelace/dist/components/rating/rating.js", JSImport.Namespace)
  @js.native
  protected object RawImport extends js.Object

  // Events

  lazy val hover  = eventProp("sl-hover")
  lazy val change = eventProp("sl-change")

  // Props

  export L.{label, value, maxAttr as max}
  export L.{disabled, readOnly}

  lazy val precision = doubleAttr("precision")
  // @JSImport("@shoelace-style/shoelace/dist/components/rating/rating.js", "getSymbol")
  // lazy val getSymbol: js.Function1[Int, String] = js.native

  def getSymbol(func: Int => String) = 
    val jsFunc: js.Function1[Int, String] = func
    callBack("getSymbol")(jsFunc)

  // def setGetSymbolFunc(func: Int => String): Mod[El] =
  //   // new:
  //   //   override def apply(element: El): Unit =
  //   //     println("set get symbol ")
  //   //     val jsFunc: js.Function1[Int, String] = func
  //   //     element.asInstanceOf[js.Dynamic].updateDynamic("getSymbol")(jsFunc)
  //   inContext { thisNode =>
  //     onMountCallback { _ =>
  //       println("set get symbol ")
  //       val jsFunc: js.Function1[Int, String] = func
  //       thisNode.ref.asInstanceOf[js.Dynamic].updateDynamic("getSymbol")(jsFunc)
  //     }
  //   }

  // -- Slots --

  @inline def noSlots: Unit = ()

  object parts:
    val base = "base"
end Rating
