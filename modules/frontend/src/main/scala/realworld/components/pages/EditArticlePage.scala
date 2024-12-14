package realworld.components.pages

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import realworld.AppState
import realworld.api.*
import realworld.components.Component
import realworld.components.widgets.ArticleEditor
import realworld.routes.{JsRouter, Page}
import realworld.spec.{Article, NotFoundError, UnprocessableEntity, UpdateArticleOutput}
import realworld.types.ArticleForm
import realworld.types.ArticleForm.u
import realworld.types.validation.GenericError
import utils.Utils.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

final case class EditArticlePage(
  pageSignal: Signal[Page.EditArticlePage]
)(using
  state: AppState,
  api: Api
) extends Component:
  val articleVar: Var[Option[Article]] = Var(None)
  val errors: Var[GenericError]        = Var(Map())
  val isSubmittingVar                  = Var(false)

  val handler = Observer[ArticleForm] { it =>
    state.authHeader
      .fold(JsRouter.redirectTo(Page.Login))(authHeader =>
        isSubmittingVar.set(true)
        it.validatedToReqData
          .foldError(
            api.articlePromise.updateArticle(authHeader, articleVar.now().get.slug, _).attempt
          )
          .collect {
            case Left(UnprocessableEntity(Some(e))) =>
              Var.set(
                isSubmittingVar -> false,
                errors          -> e
              )
            case Left(NotFoundError(msgOpt)) =>
              Var.set(
                isSubmittingVar -> false,
                errors          -> Map("Not found article, " -> List(msgOpt.getOrElse("Article not found")))
              )
            case Left(e) =>
              Var.set(
                isSubmittingVar -> false,
                errors          -> Map("error" -> List(e.getMessage()))
              )
            case Right(UpdateArticleOutput(article)) =>
              JsRouter.redirectTo(Page.ArticleDetailPage(article.slug))
          }
      )
  }

  val onLoad = pageSignal.flatMap { case Page.EditArticlePage(slug) =>
    api.promiseStream(
      _.articlePromise.getArticle(slug).map(_.article)
    )
  }.recoverToTry --> Observer[Try[Article]]:
    case Failure(exception) => JsRouter.redirectTo(Page.Home)
    case Success(article) =>
      state.titleWriter.onNext(s"Conduit: Edit ${article.title}")
      state.user.fold(JsRouter.redirectTo(Page.Login)) { u =>
        if u.username == article.author.username then articleVar.set(article.some)
        else JsRouter.redirectTo(Page.Home)
      }

  def display() =
    articleVar.signal.splitOption(
      (article, _) =>
        ArticleEditor(
          article = ArticleForm(
            article.title.some,
            article.description.some,
            article.body.some,
            article.tagList
          ),
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
