package realworld.components.widgets

import com.raquo.laminar.api.L.*
import realworld.types.InputType
import realworld.spec.TagName

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

  def TextArea(
      inputType: InputType,
      _placeholder: String,
      s_disabled: Signal[Boolean],
      isLarge: Boolean,
      _rows: Int,
      controlled: Mod[TextArea]
  ) =
    fieldSet(
      cls := "form-group",
      textArea(
        cls         := s"form-control${if isLarge then " form-control-lg" else ""}",
        tpe         := inputType.value,
        placeholder := _placeholder,
        disabled <-- s_disabled,
        rows := _rows,
        controlled
      )
    )

  def ListFormGroup(
      inputType: InputType,
      _placeholder: String,
      s_disabled: Signal[Boolean],
      isLarge: Boolean,
      controlled: Mod[Input],
      s_tags: Signal[List[TagName]],
      addTagWriter: Observer[Unit],
      removedTagWriter: Observer[TagName]
  ) =
    fieldSet(
      input(
        cls         := s"form-control${if isLarge then " form-control-lg" else ""}",
        tpe         := inputType.value,
        placeholder := _placeholder,
        disabled <-- s_disabled,
        controlled,
        onKeyDown --> { ev => if ev.key == "Enter" then ev.preventDefault() },
        onKeyUp.preventDefault.filter(_.key == "Enter").mapToUnit --> addTagWriter
      ),
      div(
        cls := "tag-list",
        children <-- s_tags.split(
          _.value
        )((tagValue, tag, _) =>
          span(
            cls := "tag-default tag-pill",
            onClick.preventDefault.mapTo(tag) --> removedTagWriter,
            i(cls := "ion-close-round"),
            tagValue
          )
        )
      )
    )

end FromGroup
