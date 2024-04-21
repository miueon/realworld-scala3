package realworld.components.pages

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import monocle.syntax.all.*
import org.scalajs.dom
import realworld.AppState
import realworld.api.Api
import realworld.components.Component
import realworld.components.widgets.ArticleViewer
import realworld.components.widgets.ArticleViewrState
import realworld.components.widgets.Favorited
import realworld.components.widgets.MyArticle
import realworld.components.widgets.Tab
import realworld.routes.Page
import realworld.spec.Article
import realworld.spec.Profile
import realworld.spec.Skip
import realworld.types.ArticlePage
import utils.Utils.writerF

import realworld.components.widgets.UserInfo
import com.raquo.laminar.nodes.ReactiveHtmlElement
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import realworld.routes.JsRouter
import utils.Utils.some
import realworld.types.ArticlePage.toPage
case class ProfileArticlePage(
    articleList: ArticlePage = ArticlePage(),
    currentPage: Int = 1
)
final case class ProfilePage(s_profile: Signal[Page.ProfilePage])(using state: AppState, api: Api)
    extends Component:
  val profileVar            = Var[Option[Profile]](None)
  val profileArticlePageVar = Var(ProfileArticlePage())
  val articlePageWriter     = profileArticlePageVar.writerF(_.focus(_.articleList).optic)
  val articlesWriter        = profileArticlePageVar.writerF(_.focus(_.articleList.articles).optic)
  val curPageWriter         = profileArticlePageVar.writerF(_.focus(_.currentPage).optic)

  val tabVar: Var[Tab] = Var(
    if dom.window.location.pathname.endsWith("favorites") then Favorited else MyArticle
  )
  def loadArticle(tab: Tab, skip: Skip = Skip(0)): EventStream[ArticlePage] =
    profileVar
      .now()
      .fold(EventStream.empty)(profile =>
        def setUrl(isFavorite: Boolean) =
          dom.window.location.hash =
            s"#/profile/${profile.username}${if isFavorite then "/favorites" else ""}"
        tab match
          case MyArticle =>
            setUrl(false)
            api
              .stream(
                _.articles.listArticle(
                  skip = skip,
                  author = profile.username.some,
                  authHeader = state.authHeader
                )
              )
              .map(_.toPage)
          case Favorited | _ =>
            setUrl(true)
            api
              .stream(
                _.articles.listArticle(
                  skip = skip,
                  favorited = profile.username.some,
                  authHeader = state.authHeader
                )
              )
              .map(_.toPage)
        end match
      )
  val tabObserver = Observer[Tab]: tab =>
    tabVar.set(tab)
    profileArticlePageVar.set(ProfileArticlePage())

  val onFavoriteObserver = Observer[Article]: article =>
    val updatedArticles =
      profileArticlePageVar.now().articleList.articles.map { (previews: List[Article]) =>
        previews.map {
          case elem if elem.slug == article.slug => article
          case elem                              => elem
        }
      }
    articlesWriter.onNext(updatedArticles)

  def displayUserInfo() =
    profileVar.signal
      .splitOption(
        (_, s_profile) => UserInfo(s_profile, profileVar.someWriter).body,
        ifEmpty = div(cls := "article-preview", "Loading profile...")
      )

  val onLoad = s_profile.flatMap { case Page.ProfilePage(username) =>
    api.stream(_.users.getProfile(username, state.authHeader).map(_.profile))
  }.recoverToTry --> Observer[Try[Profile]]:
    case Failure(exception) => JsRouter.redirectTo(Page.Home)
    case Success(profile)   => profileVar.set(profile.some)

  def body: HtmlElement =
    div(
      onLoad,
      cls := "profile-page",
      child <-- displayUserInfo(),
      div(
        cls := "container",
        div(
          cls := "row",
          div(
            cls := "col-xs-12 col-md-10 offset-md-1",
            ArticleViewer(
              profileArticlePageVar.signal.map(s =>
                ArticleViewrState(
                  s.articleList.articles,
                  s.currentPage,
                  s.articleList.articleCount.value
                )
              ),
              tabObserver,
              Signal.fromValue(List(MyArticle, Favorited)),
              "articles-toggle",
              tabVar.signal,
              curPageWriter,
              onFavoriteObserver
            ).fragement
          )
        )
      ),
      profileVar.signal
        .distinctByFn {
          case (Some(a), Some(b)) =>
            a.username == b.username
          case (_, _) => false
        }
        .changes
        .filter(_.nonEmpty)
        .flatMap(_ => loadArticle(tabVar.now())) --> articlePageWriter,
      tabVar.signal.changes.flatMap(loadArticle(_)) --> articlePageWriter,
      profileArticlePageVar.signal
        .distinctBy(_.currentPage)
        .map(_.currentPage)
        .changes
        .flatMap(a => loadArticle(tabVar.now(), Skip(a))) --> articlePageWriter
    )
  end body
end ProfilePage
