package realworld.types

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L.*

case class InputStateConfig(
  touched: Observer[Boolean] = Observer.empty,
  untouched: Observer[Boolean] = Observer.empty,
  dirty: Observer[Boolean] = Observer.empty,
  pristine: Observer[Boolean] = Observer.empty,
  valid: Observer[Boolean] = Observer.empty,
  invalid: Observer[Boolean] = Observer.empty
)

object InputStateConfig:
  def empty() = InputStateConfig()

  def inputStateMod(
    inputStateConfig: InputStateConfig,
    $invalid: Signal[Boolean]
  ): Mod[Input] =
    val $dirty                      = Var(false)
    val $touched                    = Var(false)
    val $untouched: Signal[Boolean] = $touched.signal.map(touched => !touched)
    val $pristine: Signal[Boolean]  = $dirty.signal.map(dirty => !dirty)
    val $valid                      = $invalid.map(invalid => !invalid)
    List(
      $touched --> inputStateConfig.touched,
      $untouched --> inputStateConfig.untouched,
      $dirty --> inputStateConfig.dirty,
      $pristine --> inputStateConfig.pristine,
      $valid --> inputStateConfig.valid,
      $invalid --> inputStateConfig.invalid,
      onBlur.map(_ => true) --> $touched,
      onInput.map(_ => true) --> $dirty
    )
  end inputStateMod
end InputStateConfig
