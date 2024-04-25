package realworld.components.widgets

import realworld.components.Component
import com.raquo.laminar.api.L.*
import realworld.spec.Title
import realworld.spec.Description
import realworld.spec.Body
import realworld.spec.TagName
import utils.Utils.writerF
import monocle.syntax.all.*
import realworld.types.validation.GenericError
import realworld.types.ArticleForm
import realworld.types.GenericFormField
import realworld.types.InputType
import utils.Utils.writerNTF
import realworld.types.FieldType
import utils.Utils.some

final case class ArticleEditor(
    article: ArticleForm,
    articleSubmitObserver: Observer[ArticleForm],
    s_errors: Signal[GenericError],
    s_submitting: Signal[Boolean]
) extends Component:
  val articleVar        = Var(article)
  val tagBarVar         = Var(TagName(""))
  val titleWriter       = articleVar.writerNTF(Title, _.focus(_.title).optic)
  val descriptionWriter = articleVar.writerNTF(Description, _.focus(_.description).optic)
  val bodyWriter        = articleVar.writerNTF(Body, _.focus(_.body).optic)
  val tagListWriter = articleVar
    .writerF(_.focus(_.tagList).optic)
  val addTagObserver = Observer[Unit]: _ =>
    val currentTag     = tagBarVar.now()
    val currentTagList = articleVar.now().tagList
    if !(currentTag.value.isBlank() || currentTagList.contains(currentTag)) then
      val tagList = currentTagList :+ currentTag
      tagListWriter.onNext(tagList)
      tagBarVar.set(TagName(""))

  val removeTagObserver = Observer[TagName]: t =>
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
                  value <-- articleVar.signal.map(_.title.value),
                  onInput.mapToValue --> titleWriter
                )
              ),
              GenericFormField(
                placeholder = "What's this article about?",
                controlled = controlled(
                  value <-- articleVar.signal.map(_.description.value),
                  onInput.mapToValue --> descriptionWriter
                )
              ),
              GenericFormField(
                placeholder = "Write your article (in markdown)",
                fieldType = FieldType.Textarea,
                controlled = controlled(
                  value <-- articleVar.signal.map(_.body.value),
                  onInput.mapToValue --> bodyWriter
                ),
                rows = 8.some
              ),
              GenericFormField(
                placeholder = "Enter tags",
                fieldType = FieldType.Lst,
                controlled = controlled(
                  value <-- tagBarVar.signal.map(_.value),
                  onInput.mapToValue --> tagBarVar.writer.contramap(TagName(_))
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
