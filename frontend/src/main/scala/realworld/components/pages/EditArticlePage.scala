package realworld.components.pages

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import realworld.AppState
import realworld.api.Api
import realworld.components.Component
import realworld.components.widgets.ArticleEditor
import realworld.routes.JsRouter
import realworld.routes.Page
import realworld.spec.Article
import realworld.spec.UnprocessableEntity
import realworld.spec.UpdateArticleData
import realworld.spec.UpdateArticleOutput
import realworld.types.ArticleForm
import realworld.types.validation.GenericError
import utils.Utils.some

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success
import scala.util.Try
final case class EditArticlePage(s_page: Signal[Page.EditArticlePage])(using
    state: AppState,
    api: Api
) extends Component:
  val articleVar: Var[Option[Article]] = Var(None)
  val errors: Var[GenericError]        = Var(Map())
  val isSubmittingVar                  = Var(false)

  val handler = Observer[ArticleForm] { case ArticleForm(title, description, body, tagList) =>
    state.authHeader.fold(JsRouter.redirectTo(Page.Login))(authHeader =>
      isSubmittingVar.set(true)
      api
        .future(
          _.articles
            .updateArticle(
              articleVar.now().get.slug,
              UpdateArticleData(tagList, title.some, description.some, body.some),
              authHeader
            )
            .attempt
        )
        .collect {
          case Left(UnprocessableEntity(Some(e))) =>
            Var.set(
              isSubmittingVar -> false,
              errors          -> e
            )
          case Right(UpdateArticleOutput(article)) =>
            JsRouter.redirectTo(Page.ArticleDetailPage(article.slug))
        }
    )
  }

  val onLoad = s_page.flatMap { case Page.EditArticlePage(slug) =>
    api.stream(
      _.articles.getArticle(slug).map(_.article)
    )
  }.recoverToTry --> Observer[Try[Article]]:
    case Failure(exception) => JsRouter.redirectTo(Page.Home)
    case Success(article) =>
      state.user.fold(JsRouter.redirectTo(Page.Login)) { u =>
        if u.username == article.author.username then articleVar.set(article.some)
        else JsRouter.redirectTo(Page.Home)
      }

  def display() =
    articleVar.signal.splitOption(
      (article, _) =>
        ArticleEditor(
          article = ArticleForm(article.title, article.description, article.body, article.tagList),
          articleSubmitObserver = handler,
          errors.signal,
          isSubmittingVar.signal
        ).body,
      ifEmpty = div()
    )

  def body: HtmlElement =
    div(
      onLoad,
      child <-- display()
    )
end EditArticlePage
