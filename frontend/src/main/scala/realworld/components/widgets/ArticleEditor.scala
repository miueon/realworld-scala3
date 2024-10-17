package realworld.components.widgets

import com.raquo.laminar.api.L.*
import monocle.syntax.all.*
import realworld.components.Component
import realworld.types.validation.GenericError
import realworld.types.{ArticleForm, FieldType, GenericFormField, InputType}
import utils.Utils.{some, writerF, writerOptF}

final case class ArticleEditor(
    article: ArticleForm,
    articleSubmitObserver: Observer[ArticleForm],
    s_errors: Signal[GenericError],
    s_submitting: Signal[Boolean]
) extends Component:
  val articleVar        = Var(article)
  val tagBarVar         = Var("")
  val titleWriter       = articleVar.writerOptF(_.focus(_.title).optic)
  val descriptionWriter = articleVar.writerOptF(_.focus(_.description).optic)
  val bodyWriter        = articleVar.writerOptF(_.focus(_.body).optic)
  val tagListWriter = articleVar
    .writerF(_.focus(_.tagList).optic)
  val addTagObserver = Observer[Unit]: _ =>
    val currentTag     = tagBarVar.now()
    val currentTagList = articleVar.now().tagList
    if !(currentTag.isBlank() || currentTagList.contains(currentTag)) then
      val tagList = currentTagList :+ currentTag
      tagListWriter.onNext(tagList)
      tagBarVar.set("")

  val removeTagObserver = Observer[String]: t =>
    val tagList = articleVar.now().tagList.filter(_ != t)
    tagListWriter.onNext(tagList)

  def body: HtmlElement =
    div(
      cls := "editor-page",
      ContainerPage(
        div(
          cls := "col-md-10 offset-md-1 col-xs-12",
          GenericForm(
            s_errors,
            onSubmit.preventDefault.mapTo(articleVar.now()) --> articleSubmitObserver,
            "Publish Article",
            s_submitting,
            List(
              GenericFormField(
                placeholder = "Article Title",
                controlled = controlled(
                  value <-- articleVar.signal.map(_.title.getOrElse("")),
                  onInput.mapToValue --> titleWriter
                )
              ),
              GenericFormField(
                placeholder = "What's this article about?",
                controlled = controlled(
                  value <-- articleVar.signal.map(_.description.getOrElse("")),
                  onInput.mapToValue --> descriptionWriter
                )
              ),
              GenericFormField(
                placeholder = "Write your article (in markdown)",
                fieldType = FieldType.Textarea,
                controlled = controlled(
                  value <-- articleVar.signal.map(_.body.getOrElse("")),
                  onInput.mapToValue --> bodyWriter
                ),
                rows = 8.some
              ),
              GenericFormField(
                placeholder = "Enter tags",
                fieldType = FieldType.Lst,
                controlled = controlled(
                  value <-- tagBarVar.signal,
                  onInput.mapToValue --> tagBarVar.writer
                ),
                s_tags = articleVar.signal.map(_.tagList).some
              )
            ),
            addItemToListObserverOpt = addTagObserver.some,
            removedTagObserverOpt = removeTagObserver.some
          ).fragement
        )
      )
    )
  end body
end ArticleEditor
