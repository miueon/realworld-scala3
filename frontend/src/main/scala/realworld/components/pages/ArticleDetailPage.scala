package realworld.components.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.modifiers.RenderableText
import com.raquo.laminar.nodes.ReactiveHtmlElement
import monocle.syntax.all.*
import org.scalajs.dom.HTMLButtonElement
import org.scalajs.dom.HTMLElement
import org.scalajs.dom.MouseEvent
import realworld.AppState
import realworld.api.Api
import realworld.components.Component
import realworld.components.widgets.TagListWidget
import realworld.routes.JsRouter
import realworld.routes.Page
import realworld.spec.Article
import realworld.spec.CommentView
import utils.Utils.classTupleToClassName
import utils.Utils.some
import utils.Utils.someWriterF
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import concurrent.ExecutionContext.Implicits.global
import com.raquo.airstream.core.Observer
import realworld.spec.Profile

case class CommentSectionState(
    comments: Option[List[CommentView]] = None,
    commentBody: String = "",
    submittingComment: Boolean = false
)
case class MetaSectionState(
    submittingFavorite: Boolean = false,
    submittingFollow: Boolean = false,
    deletingArticle: Boolean = false
)

final case class ArticleDetailPage(s_page: Signal[Page.ArticleDetailPage])(using
    state: AppState,
    api: Api
) extends Component:
  private val commentSectionVar                = Var(CommentSectionState())
  private val metaSectionVar                   = Var(MetaSectionState())
  private val articleVar: Var[Option[Article]] = Var(None)
  private val authorWriter                     = articleVar.someWriterF(_.focus(_.author).optic)

  private val onLoad = s_page.flatMap { case Page.ArticleDetailPage(slug) =>
    api.stream(a =>
      for
        articleOutput  <- a.articles.getArticle(slug, state.authHeader)
        commentsOutput <- a.comments.listComments(slug, state.authHeader)
      yield (articleOutput.article, commentsOutput.comments)
    )
  }.recoverToTry --> Observer[Try[(Article, List[CommentView])]]:
    case Failure(exception) => JsRouter.redirectTo(Page.Home)
    case Success(article, comments) =>
      Var.set(
        articleVar        -> article.some,
        commentSectionVar -> CommentSectionState(comments = comments.some)
      )

  private def display() =
    articleVar.signal
      .splitOption(
        (article, s_article) =>
          div(
            cls := "article-page",
            articlePageBanner(article, s_article),
            div(
              cls := "container page",
              div(
                cls := "row article-content",
                div(cls := "col-md-12", article.body.value),
                TagListWidget(article.tagList)
              )
            ),
            hr(),
            div(cls := "article-actions", articleMeta(article, s_article))
          ),
        ifEmpty = div("Loading article...")
      )

  def body: HtmlElement =
    div(
      onLoad,
      child <-- display()
    )

  def articlePageBanner(article: Article, s_article: Signal[Article]) =
    div(
      cls := "banner",
      div(
        cls := "container",
        h1(article.title.value),
        articleMeta(article, s_article)
      )
    )

  def articleMeta(article: Article, s_article: Signal[Article]) =
    div(
      cls := "article-meta",
      articleAuthorInfo(article),
      state.user match
        case Some(u) if u.username == article.author.username => ownerArticleMetaActions(article)
        case _ => viewerArticleMetaActions(article, s_article)
    )

  def ownerArticleMetaActions(article: Article): Seq[Modifier[ReactiveHtmlElement[HTMLElement]]] =
    List(
      button(
        cls := "btn btn-outline-secondary btn-sm",
        JsRouter.navigateTo(Page.EditArticlePage(article.slug)),
        i(cls := "ion-plus-round"),
        " Edit Article"
      ),
      " ",
      button(
        cls := "btn btn-outline-danger btn-sm",
        disabled <-- metaSectionVar.signal.map(_.deletingArticle),
        i(cls := "ion-heart"),
        " Delete Article",
        onClick.preventDefault --> { _ =>
          metaSectionVar.update(_.copy(deletingArticle = true))
          state.authHeader.collect { case header =>
            api
              .future(_.articles.deleteArticle(slug = article.slug, authHeader = header))
              .onComplete {
                case Failure(exception) => JsRouter.redirectTo(Page.Login)
                case Success(value)     => JsRouter.redirectTo(Page.Home)
              }
          }
        }
      )
    )

  def viewerArticleMetaActions(
      article: Article,
      s_article: Signal[Article]
  ): Seq[Modifier[ReactiveHtmlElement[HTMLElement]]] =
    val followingVar = Var(article.author.following)
    val favoritedVar = Var(article.favorited)
    val s_following  = s_article.map(_.author.following)
    val s_favorited  = s_article.map(_.favorited)
    val onFollowObserver = Observer[MouseEvent]: _ =>
      state.authHeader.fold(JsRouter.redirectTo(Page.Register)) { case header =>
        metaSectionVar.update(_.copy(submittingFollow = true))
        api
          .future(a =>
            if followingVar.now() then
              a.users.unfollowUser(article.author.username, header).map(_.profile)
            else a.users.followUser(article.author.username, header).map(_.profile)
          )
          .onComplete {
            case Failure(exception) => JsRouter.redirectTo(Page.Login)
            case Success(profile) =>
              authorWriter.onNext(profile)
              metaSectionVar.update(_.copy(submittingFollow = false))
          }
      }

    def onFavorite(_e: MouseEvent) =
      state.authHeader.fold(JsRouter.redirectTo(Page.Register)) { case header =>
        metaSectionVar.update(_.copy(submittingFavorite = true))
        api
          .future(a =>
            if favoritedVar.now() then
              a.articles.unfavoriteArticle(article.slug, header).map(_.article)
            else a.articles.favoriteArticle(article.slug, header).map(_.article)
          )
          .onComplete {
            case Failure(exception) => JsRouter.redirectTo(Page.Login)
            case Success(article) =>
              articleVar.set(article.some)
              metaSectionVar.update(_.copy(submittingFavorite = false))
          }
      }
    List(
      s_favorited --> favoritedVar.writer,
      s_following --> followingVar.writer,
      button(
        cls := "btn btn-sm",
        cls <-- s_following.map { following =>
          classTupleToClassName(
            "btn-outline-secondary" -> !following,
            "btn-secondary"         -> following
          )
        },
        disabled <-- metaSectionVar.signal.map(_.submittingFollow),
        i(cls := "ion-plus-round"),
        child.text <-- s_following
          .combineWith(s_article)
          .map((following, article) =>
            s" ${if following then "Unfollow" else "Follow"} ${article.author.username.value}"
          ),
        onClick.preventDefault --> onFollowObserver
      ),
      " ",
      button(
        cls := "btn btn-sm",
        cls <-- s_favorited.map { favorited =>
          classTupleToClassName(
            "btn-outline-primary" -> !favorited,
            "btn-primary"         -> favorited
          )
        },
        disabled <-- metaSectionVar.signal.map(_.submittingFavorite),
        i(cls := "ion-heart"),
        child.text <-- s_favorited.map(favorited =>
          s" ${if favorited then "Unfavorite" else "Favorite"} Article"
        ),
        span(cls := "counter", child.text <-- s_article.map(_.favoritesCount.value)),
        onClick.preventDefault --> onFavorite
      )
    )
  end viewerArticleMetaActions

  def articleAuthorInfo(article: Article) =
    import typings.dateFns.formatMod
    val profilePage = Page.ProfilePage(article.author.username)
    List(
      img(JsRouter.navigateTo(profilePage)),
      div(
        cls := "info",
        a(cls    := "author", JsRouter.navigateTo(profilePage)),
        span(cls := "date", formatMod.format(article.createdAt.value.toDate, "PP"))
      )
    )

  def commentForm() =
    form(
      cls := "card comment-form",
      div(
        cls := "card-block",
        textArea(
          cls         := "form-control",
          placeholder := "Write a comment...",
          rows        := 3
        )
      ),
      div(
        cls := "card-footer",
        img(),
        button(cls := "btn btn-sm btn-primary", "Post Comment")
      )
    )

end ArticleDetailPage
