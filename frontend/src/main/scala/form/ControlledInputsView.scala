package com.raquo.app.form

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object ControlledInputsView {

  private val appStyles = List(
    marginTop.px(40),
    marginBottom.px(10)
  )

  def apply(): HtmlElement = {
    div(
      p("Controlled inputs are inputs whose value is locked to the current value of an observable. For such inputs to be editable, you need to wire the user edit events (", code("onInput"), " in case of plain text inputs) to update the controlling observable, otherwise the user's input will be ignored / reverted."),
      p("Controlled inputs are useful in situations where you need to ", b("prevent"), " or ", b("undo"), " user input, or when you want the text value to be ", b("set from two sources"), ": both from user input and from an observable."),
      p("In this example, we prevent user input that isn't all digits (try pasting \"12ab\"), and we provide a button that resets the input text. All the while we have ", code("zipVar"), " which we can query to get the input's text (to be fair if the input wasn't uncontrolled, we could just get ", code("inputElement.ref.value"), ")."),
      renderExample()
    )
  }

  def renderExample(): HtmlElement = {
    // BEGIN[controlled]
    val zipVar = Var("")
    form(
      appStyles,
      onSubmit
        .preventDefault
        .mapTo(zipVar.now()) --> (zip => dom.window.alert(zip)),
      p(
        label("Zip code: "),
        input(
          placeholder("12345"),
          maxLength(5), // HTML can help block some undesired input
          controlled(
            value <-- zipVar,
            onInput.mapToValue.filter(_.forall(Character.isDigit)) --> zipVar
          )
        ),
        button(
          typ("button"), // HTML buttons are of type "submit" by default
          "Set SF zip code",
          onClick.mapTo("94110") --> zipVar
        )
      ),
      p(
        "Your zip code: ",
        child.text <-- zipVar
      ),
      // Using the form element's onSubmit in this example,
      // but you could also respond on button click if you
      // don't want a form element
      button(typ("submit"), "Submit")
    )
    // END[controlled]
  }
}
