package todomvc

import com.raquo.laminar.api.L.{*, given}
import utils.Utils.useImport
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TodoMvcApp:
  @JSImport("@find/**/TodoMvcApp.css")
  @js.native
  private object Stylesheet extends js.Object

  useImport(Stylesheet)

  case class TodoItem(id: Int, text: String, completed: Boolean)

  sealed abstract class Filter(val name: String, val passes: TodoItem => Boolean)

  object ShowAll       extends Filter("All", _ => true)
  object ShowActive    extends Filter("Active", !_.completed)
  object ShowCompleted extends Filter("Completed", _.completed)

  val filters = ShowAll :: ShowActive :: ShowCompleted :: Nil

  sealed trait Command
  case class Create(itemText: String)                         extends Command
  case class UpdateText(itemId: Int, text: String)            extends Command
  case class UpdateCompleted(itemId: Int, completed: Boolean) extends Command
  case class Delete(itemId: Int)                              extends Command
  case object DeleteCompleted                                 extends Command

  private val itemsVar  = Var(List[TodoItem]())
  private val filterVar = Var[Filter](ShowAll)
  private var lastId    = 1

  private val commandObserver = Observer[Command]:
    case Create(itemText) =>
      println(itemText)
      println(itemsVar)
      lastId += 1
      if filterVar.now() == ShowCompleted then filterVar.set(ShowAll)
      itemsVar.update(_ :+ TodoItem(id = lastId, text = itemText, completed = false))

    case UpdateText(itemId, text) =>
      itemsVar.update(_.map(item => if item.id == itemId then item.copy(text = text) else item))

    case UpdateCompleted(itemId, completed) =>
      itemsVar.update(
        _.map(item => if item.id == itemId then item.copy(completed = completed) else item)
      )

    case Delete(itemId) =>
      itemsVar.update(_.filterNot(_.id == itemId))

    case DeleteCompleted =>
      itemsVar.update(_.filterNot(_.completed))
  end commandObserver

  lazy val node =
    val todoItemSignal = itemsVar.signal.combineWith(filterVar.signal).mapN(_ filter _.passes)
    div(
      div(
        cls("todoapp-container u-bleed"),
        div(
          cls("todoapp"),
          div(
            cls("header"),
            h1("todos"),
            renderNewTodoList
          ),
          div(
            hideIfNoItems,
            cls("main"),
            ul(
              cls("todo-list"),
              children <-- todoItemSignal.split(_.id)(renderTodoItem) // render
            )
          ),
          renderStatusBar
        )
      )
    )
  end node

  private def renderNewTodoList =
    input(
      cls("new-todo"),
      placeholder("What needs to be done?"),
      autoFocus(true),
      onEnterPress.mapToValue.filter(_.nonEmpty).map(Create(_)).setValue("") --> commandObserver,
      //  but we still need an observer to create the subscription, so we just use an empty one.
      onEscapeKeyUp.setValue("") --> Observer.empty
    )

  private def renderTodoItem(itemId: Int, initialTodo: TodoItem, itemSigna: Signal[TodoItem]) =
    val isEditingVar = Var(false)
    val updateTextObserver = commandObserver.contramap[UpdateText] { updateCommand =>
      isEditingVar.set(false)
      updateCommand
    }
    val itemSignal = itemSigna.debugWithName("itemSignal").debugLogEvents(useJsLogger = true)
    li(
      cls <-- itemSignal.map(item => Map("completed" -> item.completed)),
      onDblClick.filter(_ => !isEditingVar.now()).mapTo(true) --> isEditingVar.writer,
      children <-- isEditingVar.signal.map[List[HtmlElement]] {
        case true =>
          val cancelObserver = isEditingVar.writer.contramap[Unit](u => false)
          renderTextupdateInput(itemId, itemSignal, updateTextObserver, cancelObserver) :: Nil
        case false =>
          List(
            renderCheckboxInput(itemId, itemSignal),
            label(child.text <-- itemSignal.map(_.text)),
            button(
              cls("destroy"),
              onClick.mapTo(Delete(itemId)) --> commandObserver
            )
          )
      }
    )
  end renderTodoItem

  private def renderTextupdateInput(
      itemId: Int,
      itemSignal: Signal[TodoItem],
      updateTextObserver: Observer[UpdateText],
      cancelObserver: Observer[Unit]
  ) =
    input(
      cls("edit"),
      defaultValue <-- itemSignal.map(_.text),
      onEscapeKeyUp.mapToUnit --> cancelObserver,
      onEnterPress.mapToValue.map(UpdateText(itemId, _)) --> updateTextObserver,
      onBlur.mapToValue.map(UpdateText(itemId, _)) --> updateTextObserver
    )
  private def renderCheckboxInput(itemId: Int, itemSignal: Signal[TodoItem]) =
    input(
      cls("toggle"),
      typ("checkbox"),
      checked <-- itemSignal.map(_.completed),
      onInput.mapToChecked.map { isChecked =>
        UpdateCompleted(itemId, isChecked)
      } --> commandObserver
    )

  private def renderStatusBar =
    footerTag(
      hideIfNoItems,
      cls("footer"),
      span(
        cls("todo-count"),
        child.text <-- itemsVar.signal
          .map(_.count(!_.completed))
          .map(pluralize(_, "item left", "items left"))
      ),
      ul(
        cls("filters"),
        filters.map(filter => li(renderFilterButton(filter)))
      ),
      child.maybe <-- itemsVar.signal.map { items =>
        if items.exists(ShowCompleted.passes) then
          Some(
            button(
              cls("clear-completed"),
              "Clear Completed",
              onClick.map(_ => DeleteCompleted) --> commandObserver
            )
          )
        else None
      }
    )

  private def renderFilterButton(filter: Filter) =
    a(
      cls.toggle("selected") <-- filterVar.signal.map(_ == filter),
      onClick.preventDefault.mapTo(filter) --> filterVar.writer,
      filter.name
    )

  private def hideIfNoItems =
    display <-- itemsVar.signal.map { items =>
      if items.nonEmpty then "" else "none"
    }

  private def pluralize(num: Int, singular: String, plural: String): String =
    s"$num ${if num == 1 then singular else plural}"

  private val onEnterPress  = onKeyPress.filter(_.keyCode == dom.KeyCode.Enter)
  private val onEscapeKeyUp = onKeyUp.filter(_.keyCode == dom.KeyCode.Escape)

end TodoMvcApp
