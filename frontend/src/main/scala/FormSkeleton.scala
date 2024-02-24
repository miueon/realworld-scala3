import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Form
import java.util.UUID

object FormSkeleton:
  private def Header(): HtmlElement =
    div(
      className := "flex mb-4 text-center text-[2rem]",
      "Rigister"
    )

  private def Register(): HtmlElement =
    button(
      className := "uppercase bg-[#34b1eb] text-white bold text-[1.125rem] p-[0.5rem,1rem,0.5rem,1rem]",
      className := "hover:bg-white hover:text-[#34b1eb] hover:border-[#34b1eb]",
      className := "duration-150 transition-all ease-in-out cursor-pointer border-[1px] border-solid border-[#34b1eb]",
      className := "mt-[2rem]",
      "register"
    )

  def RegisterForm() =
    val EmailValue            = Var("")
    val PasswordValue         = Var("")
    val RepeatedPasswordValue = Var("")

    form(
      className := "flex flex-col p-12 w-80 font-fancy-sans bg-[#f6f6f6] rounded-sm space-x-0",
      Header(),
      div(
        renderInput("E-mail", EmailValue, "text"),
        renderInput("Password", PasswordValue, "password"),
        renderInput("Repeat-Password", RepeatedPasswordValue, "password")
      ),
      Register()
    )
  end RegisterForm

  def renderInput(name: String, state: Var[String], t: String) =
    val inputId = s"${UUID.randomUUID().toString()}_${name}_input"

    div(
      input(
        className := "box-border border border-transparent border-solid border-1 pt-4 pb-2 pl-2 focus:outline-none focus:border-black",
        `type`   := t,
        idAttr   := inputId,
        nameAttr := name,
        inContext(thisNode => onInput.mapTo(thisNode.ref.value) --> state),
        cls <-- state.signal.changes.map(inputText =>
          if inputText.nonEmpty then "text-sm -top-10 left-1" else ""
        )
      ),
      label(
        className := "text-gray-500 leading-4 relative -top-7 left-2 transition-all duration-100 cursor-text",
        name,
        forId := inputId
      )
    )
  end renderInput
end FormSkeleton
