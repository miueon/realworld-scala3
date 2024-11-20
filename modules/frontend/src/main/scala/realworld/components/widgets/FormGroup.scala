package realworld.components.widgets

import com.raquo.laminar.api.L.*
import realworld.types.InputType

object FromGroup:
  def Input(
      inputType: InputType,
      _placeholder: String,
      s_disabled: Signal[Boolean],
      isLarge: Boolean,
      controlled: Mod[Input]
  ) =
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
      s_tags: Signal[List[String]],
      addTagWriter: Observer[Unit],
      removedTagWriter: Observer[String]
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
        children <-- s_tags.split(identity)((tagValue, tag, _) =>
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
