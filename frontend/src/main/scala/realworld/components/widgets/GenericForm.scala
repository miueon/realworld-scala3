package realworld.components.widgets

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.{*}
import com.raquo.laminar.modifiers.EventListener
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.Event
import realworld.components.ComponentSeq
import realworld.components.widgets.FromGroup.Input
import realworld.types.FieldType
import realworld.types.GenericFormField
import realworld.types.validation.GenericError

import realworld.types.FormRecord

final case class GenericForm(
    esg: Signal[GenericError],
    onSubmit: EventListener[Event, ? <: FormRecord],
    submitButtonText: String,
    s_disabled: Signal[Boolean],
    fields: List[GenericFormField]
) extends ComponentSeq:

  override def fragement: Seq[HtmlElement] =
    List(
      Errors(esg),
      form(
        onSubmit,
        fieldSet(
          fields.map { field =>
            field.filedType match
              case FieldType.Input =>
                Input(
                  field.tpe,
                  field.placeholder,
                  s_disabled,
                  field.isLarge,
                  field.controlled
                )
              case _ => ???
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
