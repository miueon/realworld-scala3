package realworld.components.pages
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.{*, given}
import monocle.syntax.all.*
import realworld.AppState
import realworld.api.Api
import realworld.components.Component
import realworld.components.pages.ArticlePage.toPage
import realworld.components.widgets.ArticleViewer
import realworld.components.widgets.ContainerPage
import realworld.spec.Article
import realworld.spec.ListArticleOutput
import realworld.spec.ListFeedArticleOutput
import realworld.spec.Skip
import realworld.spec.TagName
import realworld.spec.Total
import utils.Utils.some
import utils.Utils.writerF
case class ArticlePage(
    articleCount: Total = Total(0),
    articles: Option[List[Article]] = None
)
object ArticlePage:
  extension (a: ListFeedArticleOutput) def toPage = ArticlePage(a.articlesCount, a.articles.some)
  extension (a: ListArticleOutput) def toPage     = ArticlePage(a.articlesCount, a.articles.some)

case class HomeState(
    articleList: ArticlePage = ArticlePage(),
    currentPage: Int = 1
)

sealed trait Tab
case class Tag(tag: TagName) extends Tab:
  override def toString(): String = s"# ${tag.value}"
case object Feed extends Tab:
  override def toString(): String = "Your Feed"
case object GlobalFeed extends Tab:
  override def toString(): String = "Global Feed"
final case class Home()(using api: Api, state: AppState) extends Component:
  def tags = api.stream(_.tags.listTag().map(_.tags))

  val homeState: Var[HomeState] = Var(HomeState())
  val articlePageObserver       = homeState.writerF(_.focus(_.articleList).optic)

  private val tabVar = Var[Tab](GlobalFeed)
  def loadArticle(tab: Tab, skip: Skip = Skip(0)) =
    tab match
      case Tag(tag) =>
        api
          .stream(_.articles.listArticle(tag = tag.some, skip = skip))
          .map(_.toPage)
      case Feed =>
        api
          .stream(_.articles.listFeedArticle(state.authHeader.get, skip = skip))
          .map(_.toPage)
      case GlobalFeed =>
        api.stream(_.articles.listArticle(skip = skip)).map(_.toPage)

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
          _.articles.listArticle().map(_.toPage)
        ) --> articlePageObserver
      ),
      cls := "home-page",
      banner(),
      ContainerPage(
        div(
          cls := "col-md-9",
          ArticleViewer(
            homeState.signal,
            tabObserver,
            s_tabs,
            "feed-toggle",
            tabVar.signal,
            curPageObserver
          ).fragement
        ),
        div(cls := "col-md-3", homeSidebar())
      ),
      tabVar.signal.changes.flatMap(t => loadArticle(t)) --> articlePageObserver,
      // if we don't use the distincy operator here, the event would fire indefinately as every time the articlePageObserver updates would replace the currentPage either.
      homeState.signal
        .distinctBy(_.currentPage)
        .map(_.currentPage)
                .changes
        .flatMap(a => loadArticle(tabVar.now(), Skip((a - 1) * 10))) --> articlePageObserver
    )

end Home
