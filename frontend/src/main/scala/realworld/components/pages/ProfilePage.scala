package realworld.components.pages

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import realworld.components.Component
import realworld.components.widgets.ArticleViewer
import realworld.routes.Page
import realworld.types.ArticlePage
import concurrent.ExecutionContext.Implicits.global
import realworld.AppState
import realworld.api.Api
import utils.Utils.writerF
import monocle.syntax.all.*
import realworld.components.widgets.MyArticle
import realworld.components.widgets.Tab
import realworld.spec.Slug
import realworld.spec.Article
case class ProfileState(
    articleList: ArticlePage = ArticlePage(),
    currentPage: Int = 1
)
final case class ProfilePage(s_profile: Signal[Page.ProfilePage])(using state: AppState, api: Api)
    extends Component:

  val profileState      = Var(ProfileState())
  val articlePageWriter = profileState.writerF(_.focus(_.articleList).optic)
  val tabVar: Var[Tab]  = Var(MyArticle)
  private val tabObserver = Observer[Tab]: tab =>
    tabVar.set(tab)
    profileState.set(ProfileState())

  private val curPageWriter = profileState.writerF(_.focus(_.currentPage).optic)


  def body: HtmlElement =
    div(
      cls := "profile-page",
      div(
        cls := "container",
        div(
          cls := "row",
          div(
            cls := "col-xs-12 col-md-10 offset-md-1"
            // ArticleViewer()
          )
        )
      )
    )
    ???
  end body
end ProfilePage
