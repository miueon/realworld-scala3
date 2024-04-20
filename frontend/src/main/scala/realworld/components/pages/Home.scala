package realworld.components.pages
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import monocle.syntax.all.*
import realworld.AppState
import realworld.api.Api
import realworld.components.Component
import realworld.components.pages.ArticlePage.toPage
import realworld.components.widgets.ArticleViewer
import realworld.components.widgets.ArticleViewrState
import realworld.components.widgets.ContainerPage
import realworld.components.widgets.Feed
import realworld.components.widgets.GlobalFeed
import realworld.components.widgets.Tab
import realworld.components.widgets.Tag
import realworld.routes.JsRouter
import realworld.routes.Page
import realworld.spec.Article
import realworld.spec.ListArticleOutput
import realworld.spec.ListFeedArticleOutput
import realworld.spec.Skip
import realworld.spec.Slug
import realworld.spec.TagName
import realworld.spec.Total
import utils.Utils.some
import utils.Utils.writerF

import scala.util.Failure
import scala.util.Success

import concurrent.ExecutionContext.Implicits.global
import utils.Utils.toArticleViewerSkip

case class ArticlePreview(article: Article, isSubmitting: Boolean)
case class ArticlePage(
    articleCount: Total = Total(0),
    articlePreviews: Option[List[ArticlePreview]] = None
)
object ArticlePage:
  extension (a: ListFeedArticleOutput)
    def toPage = ArticlePage(a.articlesCount, a.articles.map(ArticlePreview(_, false)).some)
  extension (a: ListArticleOutput)
    def toPage = ArticlePage(a.articlesCount, a.articles.map(ArticlePreview(_, false)).some)

case class HomeState(
    articleList: ArticlePage = ArticlePage(),
    currentPage: Int = 1
)

final case class Home()(using api: Api, state: AppState) extends Component:
  def tags = api.stream(_.tags.listTag().map(_.tags))

  val homeState: Var[HomeState] = Var(HomeState())
  val articlePageObserver       = homeState.writerF(_.focus(_.articleList).optic)

  private val tabVar = Var[Tab](GlobalFeed)
  def loadArticle(tab: Tab, skip: Skip = Skip(0)) =
    tab match
      case Tag(tag) =>
        api
          .stream(
            _.articles.listArticle(tag = tag.some, skip = skip, authHeader = state.authHeader)
          )
          .map(_.toPage)
      case Feed =>
        api
          .stream(_.articles.listFeedArticle(state.authHeader.get, skip = skip))
          .map(_.toPage)
      case GlobalFeed | _ =>
        api.stream(_.articles.listArticle(skip = skip, authHeader = state.authHeader)).map(_.toPage)
  private val tabObserver = Observer[Tab] { tab =>
    tabVar.set(tab)
    homeState.set(HomeState())
  }
  private val curPageObserver =
    homeState.writerF(_.focus(_.currentPage).optic)
  private val s_tabs: Signal[Seq[Tab]] = tabVar.signal.map { t =>
    if state.authHeader.isDefined then Set(Feed, GlobalFeed, t).toSeq
    else Set(GlobalFeed, t).toSeq
  }

  private val onFavoriteObserver = Observer[Article]: article =>
    state.authHeader.fold(JsRouter.redirectTo(Page.Login))(authHeader =>
      starSubmittingFav(article.slug, true)
      api
        .future(a =>
          if article.favorited then
            a.articles.unfavoriteArticle(article.slug, authHeader).map(_.article)
          else a.articles.favoriteArticle(article.slug, authHeader).map(_.article)
        )
        .onComplete {
          case Failure(_)   => JsRouter.redirectTo(Page.Login)
          case Success(rsp) => endSubmittingFav(article.slug, rsp)
        }
    )

  private def starSubmittingFav(slug: Slug, isSubmitting: Boolean) =
    updatePreviews(slug, elem => elem.copy(isSubmitting = isSubmitting))

  private def updatePreviews(slug: Slug, articleUpdater: ArticlePreview => ArticlePreview) =
    val homeStateNow = homeState.now()
    val len          = homeStateNow.focus(_.articleList.articlePreviews).optic
    val updatedPreview = homeStateNow.articleList.articlePreviews.map {
      (previews: List[ArticlePreview]) =>
        previews.map {
          case elem if elem.article.slug == slug => articleUpdater(elem)
          case elem                              => elem
        }
    }
    homeState.set(len.replace(updatedPreview)(homeStateNow))

  private def endSubmittingFav(slug: Slug, article: Article) =
    updatePreviews(slug, _ => ArticlePreview(article, false))

  def banner() =
    div(
      cls := "banner",
      div(
        cls := "container",
        h1(cls := "logo-font", "conduit"),
        p("A place to share your knowledge.")
      )
    )

  def homeSidebar() =
    div(
      cls := "sidebar",
      p("Popular Tags"),
      child <-- tags.toWeakSignal
        .splitOption(
          (initial, _) =>
            div(
              cls := "tag-list",
              initial.map { tag =>
                a(
                  cls  := "tag-pill tag-default",
                  href := "#",
                  tag.value,
                  onClick.mapTo(Tag(tag)) --> tabObserver
                )
              }
            ),
          ifEmpty = span("Loading tags...")
        )
    )

  override def body: HtmlElement =
    div(
      onMountBind(el =>
        api.stream(
          _.articles.listArticle(authHeader = state.authHeader).map(_.toPage)
        ) --> articlePageObserver
      ),
      cls := "home-page",
      banner(),
      ContainerPage(
        div(
          cls := "col-md-9",
          ArticleViewer(
            homeState.signal.map(s =>
              ArticleViewrState(
                s.articleList.articlePreviews,
                s.currentPage,
                s.articleList.articleCount.value
              )
            ),
            tabObserver,
            s_tabs,
            "feed-toggle",
            tabVar.signal,
            curPageObserver,
            onFavoriteObserver
          ).fragement
        ),
        div(cls := "col-md-3", homeSidebar())
      ),
      tabVar.signal.changes.flatMap(loadArticle(_)) --> articlePageObserver,
      // if we don't use the distincy operator here, the event would fire indefinately as every time the articlePageObserver updates would replace the currentPage either.
      homeState.signal
        .distinctBy(_.currentPage)
        .map(_.currentPage)
        .changes
        .flatMap(a => loadArticle(tabVar.now(), a.toArticleViewerSkip)) --> articlePageObserver
    )

end Home
