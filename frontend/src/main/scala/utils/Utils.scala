package utils

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import monocle.Lens
import org.scalajs.dom
import realworld.spec.AuthHeader
import realworld.spec.Skip
import realworld.spec.Token
import smithy4s.Newtype

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.js
import scala.util.Failure
import scala.util.Success

object Utils:

  /** Marks the otherwise-unused import as "used" in Scala.js, preventing dead code elimination.
    */
  def useImport(importedObject: js.Any): Unit = ()

  // #TODO: Change the HtmlMod type in Laminar to accept a type param?

  type HtmlModifier[-El <: dom.html.Element] = Modifier[ReactiveHtmlElement[El]]

  object HtmlModifier:

    type Base = Modifier[ReactiveHtmlElement.Base]

  extension [A](a: A)
    def some: Some[A] = Some(a)
    def toSignal      = Signal.fromValue(a)

  extension [A](fa: Future[A])
    def attempt(using ec: ExecutionContext) = fa.transform{
      case Success(result) => Success(Right(result))
      case Failure(exception) => Success(Left(exception))
    }

  extension (a: HtmlElement) def toList = List(a)

  // Eh maybe I should add something like it to Laminar
  extension [El <: ReactiveHtmlElement.Base](mod: Modifier[El])
    def when(cond: Boolean): Modifier[El] =
      if cond then mod else emptyMod

  extension [A](obs: Observer[Option[A]])
    def emitIfValid(validator: A => Option[A]) =
      obs.contramap((value: A) => if validator(value).isEmpty then Some(value) else None)

  extension [A](signal: Signal[A])
    def validateIf(actionSignal: Signal[Boolean]) =
      actionSignal.combineWith(signal).map {
        case (activationSignal, signal) if activationSignal => signal
        case _                                              =>
      }

  extension (signal: Signal[Boolean])
    def and(secSignal: Signal[Boolean]) =
      signal.combineWith(secSignal).map { case (signal, secSignal) => signal && secSignal }

    def or(secSignal: Signal[Boolean]) =
      signal.combineWith(secSignal).map { case (signal, secSignal) => signal || secSignal }

  type Validator = Signal[Option[String]]

  def collectErrors(validators: Seq[Validator]) =
    Signal
      .combineSeq(validators)
      .map(validatorSeq =>
        validatorSeq.collect { case Some(errorMsg) =>
          errorMsg
        }
      )

  val defaultAvatarUrl = "https://api.realworld.io/images/demo-avatar.png"

  extension [A, T](sv: Var[A])
    def writerF(f: A => Lens[A, T]) =
      sv.updater[T] { case (state, cur) =>
        val lens = f(state)
        lens.replace(cur)(state)
      }

    def writerNTF(nt: Newtype[T], f: A => Lens[A, nt.Type]) =
      sv.updater[T] { case (state, cur) =>
        val lens = f(state)
        val v    = nt.apply(cur)
        lens.replace(v)(state)
      }

    def writerOptNTF(nt: Newtype[T], f: A => Lens[A, Option[nt.Type]]) =
      sv.updater[T] { case (state, cur) =>
        val lens = f(state)
        val v    = nt.apply(cur)
        lens.replace(v.some)(state)
      }

    def controlledNTF(nt: Newtype[String], f: A => Lens[A, nt.Type]) =
      controlled(
        value <-- sv.signal.map(cj => f(cj).get(cj).value),
        onInput.mapToValue --> sv.writerNTF(nt, f)
      )
  end extension

  extension [A, T](sv: Var[Option[A]])
    def someWriterF(f: A => Lens[A, T]) =
      sv.updater[T] {
        case (None, _) => None
        case (Some(state), cur) =>
          val lens = f(state)
          lens.replace(cur)(state).some
      }

  def classTupleToClassName(obj: (String, Boolean)*) =
    obj.toMap.filter(_._2).keys.mkString(" ")

  extension (tokenOpt: Option[Token])
    def toAuthHeader = tokenOpt match
      case None      => AuthHeader("")
      case Some(tok) => AuthHeader(s"Token $tok")

  extension (a: Int)
    def toArticleViewerSkip =
      Skip((a - 1) * 10)
end Utils
