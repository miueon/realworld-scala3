package realworld.components.widgets

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import realworld.spec.TagName
import realworld.components.ComponentSeq
import realworld.spec.Article
import realworld.spec.Slug
import _root_.utils.Utils.*
import org.scalajs.dom.HTMLElement
import realworld.components.pages.Tab
import com.raquo.airstream.state.StrictSignal
import realworld.components.pages.HomeState

final case class ArticleViewer(
    s_homeState: StrictSignal[HomeState],
    tabObserver: Observer[Tab],
    s_tabs: Signal[Seq[Tab]],
    toggleClassName: String,
    selectedTab: StrictSignal[Tab],
    curPageObserver: Observer[Int]
) extends ComponentSeq:
  import typings.dateFns.formatMod
  def articlePreview(s: Slug, article: Article, s_article: Signal[Article]) =
    div(
      cls := "article-preview",
      div(
        cls := "article-meta",
        a(
          cls := "author",
          img(
            src := article.author.image
              .map(_.value)
              .getOrElse("https://api.realworld.io/images/demo-avatar.png")
          )
        ),
        div(
          cls := "info",
          a(cls    := "author", href := "/", article.author.username.value),
          span(cls := "date", formatMod.format(article.createdAt.value.toDate, "PP"))
        ),
        button(
          cls := s"btn btn-sm pull-xs-right ${
              if article.favorited then "btn-primary" else "btn-outline-primary"
            }",
          aria.label := "Toggle Favorite",
          // disabled := ""
          i(cls := "ion-heart")
        )
      ),
      a(
        href := s"/#/article/${article.slug}",
        cls  := "preview-link",
        h1(article.title.value),
        p(article.description.value),
        span("Read more..."),
        TagList(article.tagList)
      )
    )

  def articleDisplay() =
    // TODO is there any better way to deal with this?
    s_homeState
      .map(_.articleList.articles)
      .toWeakSignal
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
      ArticleTabSet(s_tabs, toggleClassName, selectedTab, tabObserver),
      children <-- articleDisplay(),
      Pagination(
        s_homeState.map(_.currentPage),
        s_homeState.map(_.articleList.articleCount.value),
        itemsPerPage = 20,
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
      cls  := classTupleToClassName(Map("nav-line" -> true, "active" -> active)),
      href := "#",
      onClick.preventDefault.mapTo(tab) --> tabObserver,
      tab.toString()
    )
  )

def TagList(tagList: List[TagName]) =
  ul(
    cls := "tag-list",
    tagList.map(tag =>
      li(
        cls := "tag-default tag-pill tag-outline",
        tag.value
      )
    )
  )
