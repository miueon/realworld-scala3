package realworld.components.widgets

import com.raquo.laminar.api.L.{*, given}
import realworld.types.InputType

object FromGroup:
  def Input(
      inputType: InputType,
      _placeholder: String,
      s_disabled: Signal[Boolean],
      isLarge: Boolean,
      controlled: Mod[Input]
  ) =
    //   // def test(): Mod[ReactiveHtmlElement[HTMLInputElement]] = Binder { el =>
    //   //   val sing = Var("")
    //   //   (onInput.mapToValue --> sing.writer).bind(el)
    //   // }
    fieldSet(
      cls := "form-group",
      input(
        cls         := s"form-control${if isLarge then " form-control-lg" else ""}",
        tpe         := inputType.value,
        placeholder := _placeholder,
        disabled <-- s_disabled,
        controlled
      )
    )
  end Input

      
end FromGroup
