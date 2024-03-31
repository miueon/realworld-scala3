package realworld.components.pages
import com.raquo.laminar.api.L.{*, given}
import realworld.components.widgets.ContainerPage
import realworld.api.Api
import realworld.components.Component
import realworld.spec.Article
import com.raquo.laminar.nodes.ReactiveElement
import realworld.spec.Slug
import utils.Utils.*
import realworld.routes.JsRouter
import realworld.components.widgets.TagList

final case class Home()(using api: Api) extends Component:
  def articles =
    api.stream(_.articles.listArticle().map(_.articles))

  def banner() =
    div(
      cls := "banner",
      div(
        cls := "container",
        h1(cls := "logo-font", "conduit"),
        p("A place to share your knowledge.")
      )
    )
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
  

  def articleDisplay(articles: EventStream[List[Article]]) =
    // TODO is there any better way to deal with this?
    articles.toWeakSignal
      .splitOption(
        (initial, s) =>
          if initial.isEmpty then
            div(cls := "article-preview", "No articles are here... yet.").toList.toSignal
          else s.split(_.slug)(articlePreview),
        ifEmpty = div(cls := "article-preview", "Loading articles...").toList.toSignal
      )
      .flatten

  override def body: HtmlElement =
    div(
      cls := "home-page",
      banner(),
      ContainerPage(
        div(cls := "col-md-9", children <-- articleDisplay(articles))
      )
    )

end Home
