package realworld
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.SplitRender
import realworld.api.Api
import realworld.components.pages.Home
import realworld.components.pages.Login
import realworld.components.pages.Register
import realworld.components.widgets.Footer
import realworld.components.widgets.Header
import realworld.routes.JsRouter
import realworld.routes.Page
import realworld.components.Component
import utils.Utils.toSignal
import realworld.components.pages.EditArticlePage
import realworld.components.pages.ProfilePage
import realworld.components.pages.Settings
import realworld.components.pages.NewArticle
import realworld.components.pages.ArticleDetailPage

object App:
  extension [P](sp: SplitRender[P, HtmlElement])
    def collectComponent(p: P)(c: => Component) =
      sp.collectStatic(p)(c.body)

  def renderPage(using state: AppState, api: Api): Signal[HtmlElement] =
    SplitRender(JsRouter.currentPageSignal.distinct)
      .collectComponent(Page.Home)(Home())
      .collectComponent(Page.Login)(Login())
      .collectComponent(Page.Register)(Register())
      .collectComponent(Page.Setting)(Settings())
      .collectComponent(Page.NewArticle)(NewArticle())
      .collectSignal[Page.ArticleDetailPage](ArticleDetailPage(_).body)
      .collectSignal[Page.EditArticlePage](EditArticlePage(_).body)
      .collectSignal[Page.ProfilePage](ProfilePage(_).body)
      .signal

  def main() =
    given Api             = Api.create()
    given state: AppState = AppState.init

    val node = state.s_token.changes
      .splitOption(
        (initial, _) => renderPage,
        div().toSignal
      )
      .flatten

    div(
      AuthStateWatcher(state.events).loop,
      onMountCallback(_ => state.events.emit(AuthEvent.Load)),
      Header(),
      child <-- node,
      Footer()
    )
  end main
end App

def authenticatedOnly(using state: AppState) =
  state.s_token --> { tok =>
    tok match
      case None | Some(AuthState.Unauthenticated) => JsRouter.redirectTo(Page.Login)
      case _                                      =>
  }

def guestOnly(using state: AppState) =
  state.s_token --> { tok =>
    tok match
      case None | Some(AuthState.Unauthenticated) =>
      case _                                      => JsRouter.redirectTo(Page.Home)
  }
