package realworld.components.widgets

import _root_.utils.Utils.*
import com.raquo.airstream.state.StrictSignal
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLElement
import realworld.AppState
import realworld.api.*
import realworld.components.ComponentSeq
import realworld.routes.{JsRouter, Page}
import realworld.spec.{Article, Slug}
import realworld.types.TagName
import utils.Utils

import scala.util.{Failure, Success}

import concurrent.ExecutionContext.Implicits.global
case class ArticleViewrState(
    articlePreviews: Option[List[Article]],
    currentPage: Int,
    articleCount: Int
)
sealed trait Tab
case class Tag(tag: TagName) extends Tab:
  override def toString(): String = s"# $tag"
case object Feed extends Tab:
  override def toString(): String = "Your Feed"
case object GlobalFeed extends Tab:
  override def toString(): String = "Global Feed"
case object Favorited extends Tab:
  override def toString(): String = "Favorited Articles"
case object MyArticle extends Tab:
  override def toString(): String = "My Articles"
final case class ArticleViewer(
    viewerStateSignal: Signal[ArticleViewrState],
    tabObserver: Observer[Tab],
    tabsSignal: Signal[Seq[Tab]],
    toggleClassName: String,
    selectedTab: StrictSignal[Tab],
    curPageObserver: Observer[Int],
    onFavArticleObserver: Observer[Article]
)(using state: AppState, api: Api)
    extends ComponentSeq:
  import typings.dateFns.formatMod
  def articlePreview(
      s: Slug,
      article: Article,
      articleSignal: Signal[Article]
  ) =
    val articleVar      = Var(article)
    val isSubmittingVar = Var(false)
    def favObserver = Observer[Article] { article =>
      isSubmittingVar.set(true)
      state.authHeader.fold(JsRouter.redirectTo(Page.Login))(authHeader =>
        api
          .promise(
            if article.favorited then
              _.articlePromise.unfavoriteArticle(authHeader, article.slug).map(_.article)
            else _.articlePromise.favoriteArticle(authHeader, article.slug).map(_.article)
          )
          .onComplete {
            case Failure(_) => JsRouter.redirectTo(Page.Login)
            case Success(article) =>
              isSubmittingVar.set(false)
              onFavArticleObserver.onNext(article)
          }
      )
    }
    div(
      articleSignal --> articleVar.writer,
      cls := "article-preview",
      div(
        cls := "article-meta",
        a(
          cls := "author",
          img(
            src <-- articleSignal.map(
              _.author.image
                .getOrElse(Utils.defaultAvatarUrl)
            )
          )
        ),
        div(
          cls := "info",
          a(cls    := "author", href := "/", article.author.username),
          span(cls := "date", formatMod.format(article.createdAt.value.toDate, "PP"))
        ),
        button(
          cls := "btn btn-sm pull-xs-right",
          cls <-- articleSignal.map(a => if a.favorited then "btn-primary" else "btn-outline-primary"),
          aria.label := "Toggle Favorite",
          disabled <-- isSubmittingVar.signal,
          i(cls := "ion-heart"),
          child.text <-- articleSignal.map(a => s" ${a.favoritesCount.value}"),
          onClick.preventDefault.mapTo(articleVar.now()) --> favObserver
        )
      ),
      a(
        JsRouter.navigateTo(Page.ArticleDetailPage(article.slug)),
        cls := "preview-link",
        h1(article.title),
        p(article.description),
        span("Read more..."),
        TagListWidget(article.tagList)
      )
    )
  end articlePreview

  def articleDisplay() =
    // TODO is there any better way to deal with this?
    viewerStateSignal
      .map(_.articlePreviews)
      .signal
      .splitOption(
        (initial, s) =>
          if initial.isEmpty then
            div(cls := "article-preview", "No articles are here... yet.").toList.toSignal
          else s.split(_.slug)(articlePreview),
        ifEmpty = div(cls := "article-preview", "Loading articles...").toList.toSignal
      )
      .flatten

  def fragement: Seq[Modifier[ReactiveHtmlElement[HTMLElement]]] =
    List(
      ArticleTabSet(tabsSignal, toggleClassName, selectedTab, tabObserver),
      children <-- articleDisplay(),
      Pagination(
        viewerStateSignal.map(_.currentPage),
        viewerStateSignal.map(_.articleCount),
        itemsPerPage = 10,
        curPageObserver
      )
    )
end ArticleViewer

def ArticleTabSet(
    tabsSignal: Signal[Seq[Tab]],
    toggleClassName: String,
    selectedTab: StrictSignal[Tab],
    tabObserver: Observer[Tab]
) =
  div(
    cls := toggleClassName,
    ul(
      cls := "nav nav-pills outline-active",
      children <-- tabsSignal.map(_.map(tab => Tab(tab, tab.equals(selectedTab.now()), tabObserver)))
    )
  )

def Tab(tab: Tab, active: Boolean, tabObserver: Observer[Tab]) =
  li(
    cls := "nav-item",
    a(
      cls  := classTupleToClassName("nav-link" -> true, "active" -> active),
      href := "#",
      onClick.preventDefault.mapTo(tab) --> tabObserver,
      tab.toString()
    )
  )

def TagListWidget(tagList: List[TagName]) =
  ul(
    cls := "tag-list",
    tagList.map(tag =>
      li(
        cls := "tag-default tag-pill tag-outline",
        tag
      )
    )
  )
