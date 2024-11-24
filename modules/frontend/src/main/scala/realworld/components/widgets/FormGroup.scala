package realworld.components.widgets

import com.raquo.laminar.api.L.*
import realworld.types.InputType

object FromGroup:
  def Input(
      inputType: InputType,
      _placeholder: String,
      isDisabledSignal: Signal[Boolean],
      isLarge: Boolean,
      controlled: Mod[Input]
  ) =
    fieldSet(
      cls := "form-group",
      input(
        cls         := s"form-control${if isLarge then " form-control-lg" else ""}",
        tpe         := inputType.value,
        placeholder := _placeholder,
        disabled <-- isDisabledSignal,
        controlled
      )
    )
  end Input

  def TextArea(
      inputType: InputType,
      _placeholder: String,
      isDisabledSignal: Signal[Boolean],
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
        disabled <-- isDisabledSignal,
        rows := _rows,
        controlled
      )
    )

  def ListFormGroup(
      inputType: InputType,
      _placeholder: String,
      isDisabledSignal: Signal[Boolean],
      isLarge: Boolean,
      controlled: Mod[Input],
      tagsSignal: Signal[List[String]],
      addTagWriter: Observer[Unit],
      removedTagWriter: Observer[String]
  ) =
    fieldSet(
      input(
        cls         := s"form-control${if isLarge then " form-control-lg" else ""}",
        tpe         := inputType.value,
        placeholder := _placeholder,
        disabled <-- isDisabledSignal,
        controlled,
        onKeyDown --> { ev => if ev.key == "Enter" then ev.preventDefault() },
        onKeyUp.preventDefault.filter(_.key == "Enter").mapToUnit --> addTagWriter
      ),
      div(
        cls := "tag-list",
        children <-- tagsSignal.split(identity)((tagValue, tag, _) =>
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
