package realworld.components.widgets

import _root_.utils.Utils.*
import com.raquo.airstream.state.StrictSignal
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLElement
import realworld.components.ComponentSeq
import realworld.spec.Slug
import realworld.spec.TagName
import realworld.components.pages.ArticlePreview
import realworld.spec.Article
import realworld.routes.JsRouter
import realworld.routes.Page

case class ArticleViewrState(
    articlePreviews: Option[List[ArticlePreview]],
    currentPage: Int,
    articleCount: Int
)
sealed trait Tab
case class Tag(tag: TagName) extends Tab:
  override def toString(): String = s"# ${tag.value}"
case object Feed extends Tab:
  override def toString(): String = "Your Feed"
case object GlobalFeed extends Tab:
  override def toString(): String = "Global Feed"
final case class ArticleViewer(
    s_viewerState: Signal[ArticleViewrState],
    tabObserver: Observer[Tab],
    s_tabs: Signal[Seq[Tab]],
    toggleClassName: String,
    selectedTab: StrictSignal[Tab],
    curPageObserver: Observer[Int],
    onFavorite: (Article) => Unit = (_) => ()
) extends ComponentSeq:
  import typings.dateFns.formatMod
  def articlePreview(
      s: Slug,
      articlePreview: ArticlePreview,
      s_articlePreview: Signal[ArticlePreview]
  ) =
    val articleVar = Var(articlePreview.article)
    val article    = articlePreview.article
    val s_article  = s_articlePreview.map(_.article)
    div(
      s_article --> articleVar.writer,
      cls := "article-preview",
      div(
        cls := "article-meta",
        a(
          cls := "author",
          img(
            src <-- s_article.map(
              _.author.image
                .map(_.value)
                .getOrElse("https://api.realworld.io/images/demo-avatar.png")
            )
          )
        ),
        div(
          cls := "info",
          a(cls    := "author", href := "/", article.author.username.value),
          span(cls := "date", formatMod.format(article.createdAt.value.toDate, "PP"))
        ),
        button(
          cls := "btn btn-sm pull-xs-right",
          cls <-- s_article.map(a => if a.favorited then "btn-primary" else "btn-outline-primary"),
          aria.label := "Toggle Favorite",
          disabled <-- s_articlePreview.map(_.isSubmitting),
          i(cls := "ion-heart"),
          child.text <-- s_article.map(a => s" ${a.favoritesCount.value}"),
          onClick.preventDefault --> { _ =>
            onFavorite(articleVar.now())
          }
        )
      ),
      a(
        JsRouter.navigateTo(Page.ArticleDetailPage(article.slug)),
        cls := "preview-link",
        h1(article.title.value),
        p(article.description.value),
        span("Read more..."),
        TagListWidget(article.tagList),
        test
      )
    )
  end articlePreview

  def test = 1

  def articleDisplay() =
    // TODO is there any better way to deal with this?
    s_viewerState
      .map(_.articlePreviews)
      .signal
      .splitOption(
        (initial, s) =>
          if initial.isEmpty then
            div(cls := "article-preview", "No articles are here... yet.").toList.toSignal
          else s.split(_.article.slug)(articlePreview),
        ifEmpty = div(cls := "article-preview", "Loading articles...").toList.toSignal
      )
      .flatten

  def fragement: Seq[Modifier[ReactiveHtmlElement[HTMLElement]]] =
    List(
      ArticleTabSet(s_tabs, toggleClassName, selectedTab, tabObserver),
      children <-- articleDisplay(),
      Pagination(
        s_viewerState.map(_.currentPage),
        s_viewerState.map(_.articleCount),
        itemsPerPage = 10,
        curPageObserver
      )
    )
end ArticleViewer

def ArticleTabSet(
    s_tabs: Signal[Seq[Tab]],
    toggleClassName: String,
    selectedTab: StrictSignal[Tab],
    tabObserver: Observer[Tab]
) =
  div(
    cls := toggleClassName,
    ul(
      cls := "nav nav-pills outline-active",
      children <-- s_tabs.map(_.map(tab => Tab(tab, tab.equals(selectedTab.now()), tabObserver)))
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
        tag.value
      )
    )
  )
