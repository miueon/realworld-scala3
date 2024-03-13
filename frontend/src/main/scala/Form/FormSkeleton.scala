package Form
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js
import utils.Utils.useImport
import Input.InputType.PASSOWRD
import Input.InputType.TEXT
import com.raquo.airstream.core.Observer
import _root_.Form.Helper.emitIfValid
import _root_.Form.Helper.SignalValidation
import _root_.Form.Helper.BooleanOps
object FormSkeleton:
  @JSImport("@find/**/Form.less", JSImport.Namespace)
  @js.native
  private object Stylesheet extends js.Object

  useImport(Stylesheet)

  private def Header(): HtmlElement =
    div(
      cls := "header",
      "Rigister"
    )

  private def Register(): HtmlElement =
    button(
      cls := "register",
      "register"
    )

  case class FormState(email: Option[String], password: Option[String])

  def RegisterForm() =
    // val s_emailValue            = Var("")
    // val s_passwordValue         = Var("")
    // val s_repeatedPasswordValue = Var("")

    val s_formSend  = Var(false)
    val s_formState = Var(FormState(None, None))

    div(
      cls := "FormStyles",
      form(
        cls := "form",
        Header(),
        // div(
        //   Input.Email(
        //     "E-mail",
        //     "email",
        //     s_emailValue.writer,
        //     Signal.fromValue(Some("Invalid e-mail"))
        //   ),
        //   Input.Password("Password", "password", s_passwordValue.writer),
        //   Input.Password("Repeat-Password", "repeat-password", s_repeatedPasswordValue.writer)
        // ),
        onSubmit.preventDefault.map(_ => true) --> s_formSend,
        Email(
          s_formSend.signal,
          s_formState.updater((state, newEmail) => state.copy(email = newEmail))
        ),
        Passwords(
          s_formSend.signal,
          s_formState.updater((state, newPassword) => state.copy(password = newPassword))
        ),
        Register()
      )
    )
  end RegisterForm

  private def Email(s_formSend: Signal[Boolean], s_emailObserver: Observer[Option[String]]) =
    val s_email         = Var("")
    val s_emailTourched = Var(false)
    Input.Email(
      "E-mail",
      "email",
      Observer.combine(
        s_emailObserver.emitIfValid(isEmailValid),
        s_email.writer
      ),
      InputStateConfig(touched = s_emailTourched.writer),
      // s_email.signal.map(isEmailValid).combineWith(s_formSend, s_emailTourched.signal).map {
      //   case (emailValidationResult, formSend, emailTourched) =>
      //     if formSend || emailTourched then emailValidationResult else None
      // }
      s_email.signal.map(isEmailValid).validateIf(s_formSend or s_emailTourched.signal)
    )
  end Email

  private def Passwords(
      $formSend: Signal[Boolean],
      $passwordObserver: Observer[Option[String]]
  ): List[HtmlElement] =
    import Helper.*
    val $password = Var[String]("")
    val firstPassword =
      val $firstTouched = Var(false)
      Input.Password(
        "Password",
        "password",
        $password.writer,
        InputStateConfig(touched = $firstTouched.writer),
        $password.signal.map(isPasswordEmpty).validateIf($formSend or $firstTouched.signal)
      )
    val secondPassword =
      val $secondTouched  = Var(false)
      val $secondValid    = Var(false)
      val $secondPassword = Var[String]("")
      val $doPasswordMatch: Signal[Option[String]] = $password.signal
        .combineWith($secondPassword.signal)
        .map(doPasswordsMatch.tupled)
        .validateIf($formSend or $secondTouched.signal)

      val passwordObserverValidation = (secondPassword: String) =>
        doPasswordsMatch($password.signal.now(), secondPassword)
          .orElse(isPasswordEmpty(secondPassword))

      Input.Password(
        "Repeat password",
        "repeated-password",
        Observer.combine(
          $passwordObserver.emitIfValid(passwordObserverValidation),
          $secondPassword.writer
        ),
        InputStateConfig(touched = $secondTouched.writer, valid = $secondValid.writer),
        $secondPassword.signal.map(isPasswordEmpty).validateIf($formSend or $secondTouched.signal),
        $doPasswordMatch
      )
    end secondPassword
    List(
      firstPassword,
      secondPassword
    )
  end Passwords

  private val isEmailValid = (password: String) =>
    if password.nonEmpty then
      if password.contains('@') then None
      else Some("E-mail has invalid format.")
    else Some("E-mail can't be empty.")

  private val doPasswordsMatch = (password: String, repeatedPassword: String) =>
    if password == repeatedPassword then None
    else Some("Passwords doesn't match")

  private val isPasswordEmpty = (password: String) =>
    if password.nonEmpty then None
    else Some("Password can't be empty.")
end FormSkeleton

object Helper:
  extension [A](obs: Observer[Option[A]])
    def emitIfValid(validator: A => Option[A]) =
      obs.contramap((value: A) => if validator(value).isEmpty then Some(value) else None)

  implicit class SignalValidation[A]($signal: Signal[Option[A]]):
    def validateIf($activationSignal: Signal[Boolean]): Signal[Option[A]] =
      $activationSignal.combineWith($signal).map({
        case (activationSignal, signal) if activationSignal => signal
        case _                                              => None
      })

  implicit class BooleanOps($signal: Signal[Boolean]):
    def and($secondSignal: Signal[Boolean]): Signal[Boolean] =
      $signal.combineWith($secondSignal)
        .map({ case (signal, secondSignal) => signal && secondSignal })

    def or($secondSignal: Signal[Boolean]): Signal[Boolean] =
      $signal.combineWith($secondSignal)
        .map({ case (signal, secondSignal) => signal || secondSignal })
end Helper

object Input:
  opaque type InputType = String
  object InputType:
    val PASSOWRD: InputType = "password"
    val TEXT: InputType     = "text"
  def Password(
      lableText: String,
      id: String,
      valueObserver: Observer[String],
      inputStateConfig: InputStateConfig,
      validators: Validator*
  ): HtmlElement =
    Input(lableText, id, PASSOWRD, valueObserver, inputStateConfig, validators)

  def Email(
      lableText: String,
      id: String,
      valueObserver: Observer[String],
      inputStateConfig: InputStateConfig,
      validators: Validator*
  ): HtmlElement =
    Input(lableText, id, TEXT, valueObserver, inputStateConfig, validators)

  private def Input(
      lableText: String,
      id: String,
      inputType: InputType,
      valueObserver: Observer[String],
      inputStateConfig: InputStateConfig,
      validators: Seq[Validator]
  ): HtmlElement =
    val s_value           = Var("")
    val s_collectedErrors = collectErrors(validators)
    val s_invalid         = s_collectedErrors.map(_.nonEmpty)
    div(
      cls := "InputStyles",
      div(
        cls := "inputWrapper",
        div(
          cls.toggle("invalid") <-- s_invalid,
          cls := "Input",
          input(
            inputStateMod(inputStateConfig, s_invalid),
            `type`   := inputType,
            idAttr   := id,
            nameAttr := id,
            onInput.mapToValue --> valueObserver,
            onInput.mapToValue --> s_value,
            cls.toggle("non-empty") <-- s_value.signal.map(_.nonEmpty)
          ),
          label(
            lableText,
            forId := id
          ),
          Errors(s_collectedErrors)
        )
      )
    )
  end Input

  private def collectErrors(validators: Seq[Validator]) =
    Signal
      .combineSeq(validators)
      .map(validatorSeq =>
        validatorSeq.collect { case Some(errorMsg) =>
          errorMsg
        }
      )

  private def Errors(s_collectedErrors: Signal[Seq[String]]) =
    div(
      cls := "Errors",
      children <-- s_collectedErrors.map(errors => errors.map(div(_)))
    )

  private def inputStateMod(
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
end Input

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
