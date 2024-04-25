package realworld.components.widgets

import com.raquo.laminar.api.L.*
import realworld.types.validation.GenericError

def Errors(erros: Signal[GenericError]) =
  ul(
    cls := "error-messages",
    children <-- erros.map(
      _.flatMap(error =>
        error._2.map(msg =>
          li(
            s"${error._1} $msg"
          )
        )
      ).toList
    )
  )
