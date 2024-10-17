package realworld.components.widgets

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import com.raquo.laminar.modifiers.EventListener
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.Event
import realworld.components.ComponentSeq
import realworld.components.widgets.FromGroup.{Input, ListFormGroup, TextArea}
import realworld.types.validation.GenericError
import realworld.types.{FieldType, FormRecord, GenericFormField}

final case class GenericForm(
    esg: Signal[GenericError],
    onSubmit: EventListener[Event, ? <: FormRecord],
    submitButtonText: String,
    s_disabled: Signal[Boolean],
    fields: List[GenericFormField],
    addItemToListObserverOpt: Option[Observer[Unit]] = None,
    removedTagObserverOpt: Option[Observer[String]] = None
) extends ComponentSeq:
// Use EventListener if the state is located in the parent component
// Use Callback if the state is located in the child component
// Use Obserer if we need a state machine to handle more complex state
  override def fragement =
    List(
      Errors(esg),
      form(
        onSubmit,
        fieldSet(
          fields.map { field =>
            field.fieldType match
              case FieldType.Input =>
                Input(
                  field.tpe,
                  field.placeholder,
                  s_disabled,
                  field.isLarge,
                  field.controlled
                )
              case FieldType.Textarea =>
                TextArea(
                  field.tpe,
                  field.placeholder,
                  s_disabled,
                  field.isLarge,
                  field.rows.getOrElse(3),
                  field.controlled
                )
              case FieldType.Lst =>
                ListFormGroup(
                  field.tpe,
                  field.placeholder,
                  s_disabled,
                  field.isLarge,
                  field.controlled,
                  field.s_tags.get,
                  addItemToListObserverOpt.getOrElse(Observer.empty),
                  removedTagObserverOpt.getOrElse(Observer.empty)
                )
          },
          button(
            cls := "btn btn-lg btn-primary pull-xs-right",
            submitButtonText
          )
        )
      )
    )
  end fragement
end GenericForm
