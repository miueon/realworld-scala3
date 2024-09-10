package realworld.components.pages

import com.raquo.laminar.api.L.*
import realworld.AppState
import realworld.api.*
import realworld.components.Component
import realworld.components.widgets.ArticleEditor
import realworld.routes.JsRouter
import realworld.routes.Page
import realworld.spec.CreateArticleData
import realworld.spec.CreateArticleOutput
import realworld.spec.UnprocessableEntity
import realworld.types.ArticleForm
import realworld.types.validation.GenericError
import utils.Utils.*

import scala.concurrent.ExecutionContext.Implicits.global
final case class NewArticle()(using state: AppState, api: Api) extends Component:
  val isSubmittingVar           = Var(false)
  val errors: Var[GenericError] = Var(Map())
  val handler = Observer[ArticleForm] { case ArticleForm(title, desc, body, tagList) =>
    state.authHeader.fold(JsRouter.redirectTo(Page.Login))(authHeader =>
      isSubmittingVar.set(true)
      api
        .promise(
          _.articlePromise
            .createArticle(authHeader, CreateArticleData(title, desc, body, tagList))
            .attempt
        )
        .collect {
          case Left(UnprocessableEntity(Some(e))) =>
            Var.set(
              isSubmittingVar -> false,
              errors          -> e
            )
          case Right(CreateArticleOutput(article)) =>
            JsRouter.redirectTo(Page.ArticleDetailPage(article.slug))
        }
    )
  }

  def body: HtmlElement =
    ArticleEditor(ArticleForm(), handler, errors.signal, isSubmittingVar.signal).body
end NewArticle
