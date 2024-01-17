import org.scalajs.dom
import com.raquo.laminar.api.L.*
import typings.stripeStripeJs.stripeStripeJsRequire
import typings.stripeStripeJs.typesStripeJsStripeMod.Stripe
import typings.stripeStripeJs.mod.*
import typings.stripeStripeJs.typesStripeJsElementsGroupMod.StripeElementsOptionsMode
import typings.stripeStripeJs.stripeStripeJsStrings.payment
import concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import typings.stripeStripeJs.typesStripeJsElementsGroupMod.StripeElements
import scala.concurrent.Await
import scala.concurrent.duration.Duration

val stripe = loadStripe(
  "pk_test_51MGZzUKgl38tQANAUpWQjJahe8Fs74Zf10Wvs0a1Znd03DB7e7Ny7AwSEoVNWE0HkezVT4qizp6KMlIQIY5g73Ew00nvzc0Y1F"
).toFuture
val options = new StripeElementsOptionsMode:
  amount = 1099
  mode = payment
  currency = "usd"

val elementFu: Future[StripeElements] =
  for stripe <- stripe
  yield stripe.elements(options)

val nameVar = Var("world")
val rootElement = div(
  input(
    className   := "NameInput",
    placeholder := "Enter your name here",
    inContext { thisNode =>
      onInput.map(_ => thisNode.ref.value) --> nameVar
    }
  ),
  span(
    child.text <-- nameVar.signal.map(_.toUpperCase()),
    " world"
  )
)

def Counter(label: String): HtmlElement =
  val diffBus = new EventBus[Int]

  val $count = diffBus.events.foldLeft(initial = 0)(_ + _)

  div(
    label + ": ",
    b(child.text <-- $count),
    button("-", onClick.mapTo(-1) --> diffBus),
    button("+", onClick.mapTo(1) --> diffBus)
  )

val text = div(
  h1("Let's Count!"),
  Counter("Sheep")
)

def expressCheckout =
  div(
    idAttr := "express-checkout-element"
  )

def errorMessage =
  div(
    idAttr := "error-message"
  )

def mountExpressCheckout =
  for element <- elementFu
  yield element
    .create_expressCheckout(
      typings.stripeStripeJs.stripeStripeJsStrings.expressCheckout
    )
    .mount("#express-checkout-element")

// @main
def heeloComponent() =
  renderOnDomContentLoaded(
    dom.document.getElementById("app"),
    expressCheckout
  )
  mountExpressCheckout.value
