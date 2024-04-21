package realworld.components.pages
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import monocle.syntax.all.*
import realworld.AppState
import realworld.api.Api
import realworld.components.Component
import realworld.components.widgets.ArticleViewer
import realworld.components.widgets.ArticleViewrState
import realworld.components.widgets.ContainerPage
import realworld.components.widgets.Feed
import realworld.components.widgets.GlobalFeed
import realworld.components.widgets.Tab
import realworld.components.widgets.Tag
import realworld.spec.Article
import realworld.spec.Skip
import realworld.spec.TagName
import realworld.spec.Total
import realworld.types.ArticlePage
import realworld.types.ArticlePage.toPage
import utils.Utils.some
import utils.Utils.toArticleViewerSkip
import utils.Utils.writerF

case class HomeState(
    articleList: ArticlePage = ArticlePage(),
    currentPage: Int = 1
)

final case class Home()(using api: Api, state: AppState) extends Component:
  def tags = api.stream(_.tags.listTag().map(_.tags))

  val homeState: Var[HomeState] = Var(HomeState())
  val articlePageWriter         = homeState.writerF(_.focus(_.articleList).optic)
  val articlesWriter            = homeState.writerF(_.focus(_.articleList.articles).optic)
  val curPageWriter             = homeState.writerF(_.focus(_.currentPage).optic)

  val tabVar = Var[Tab](GlobalFeed)
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
  val tabObserver = Observer[Tab] { tab =>
    tabVar.set(tab)
    homeState.set(HomeState())
  }
  val s_tabs: Signal[Seq[Tab]] = tabVar.signal.map { t =>
    if state.authHeader.isDefined then Set(Feed, GlobalFeed, t).toSeq
    else Set(GlobalFeed, t).toSeq
  }

  val onFavoriteObserver = Observer[Article]: article =>
    val updatedArticles = homeState.now().articleList.articles.map { (previews: List[Article]) =>
      previews.map {
        case elem if elem.slug == article.slug => article
        case elem                              => elem
      }
    }
    articlesWriter.onNext(updatedArticles)

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
      onMountBind(el => loadArticle(tabVar.now()) --> articlePageWriter),
      cls := "home-page",
      banner(),
      ContainerPage(
        div(
          cls := "col-md-9",
          ArticleViewer(
            homeState.signal.map(s =>
              ArticleViewrState(
                s.articleList.articles,
                s.currentPage,
                s.articleList.articleCount.value
              )
            ),
            tabObserver,
            s_tabs,
            "feed-toggle",
            tabVar.signal,
            curPageWriter,
            onFavoriteObserver
          ).fragement
        ),
        div(cls := "col-md-3", homeSidebar())
      ),
      tabVar.signal.changes.flatMap(loadArticle(_)) --> articlePageWriter,
      // if we don't use the distincy operator here, the event would fire indefinately as every time the articlePageObserver updates would replace the currentPage either.
      homeState.signal
        .distinctBy(_.currentPage)
        .map(_.currentPage)
        .changes
        .flatMap(a => loadArticle(tabVar.now(), a.toArticleViewerSkip)) --> articlePageWriter
    )
end Home
