package realworld
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.SplitRender
import realworld.api.Api
import realworld.components.Component
import realworld.components.pages.{
  ArticleDetailPage, EditArticlePage, Home, Login, NewArticle, ProfilePage, Register, Settings
}
import realworld.components.widgets.{Footer, Header}
import realworld.routes.{JsRouter, Page}
import utils.Utils.toSignal

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

    val node = state.tokenSignal.changes
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
  state.tokenSignal --> { tok =>
    tok match
      case None | Some(AuthState.Unauthenticated) => JsRouter.redirectTo(Page.Login)
      case _                                      =>
  }

def guestOnly(using state: AppState) =
  state.tokenSignal --> { tok =>
    tok match
      case None | Some(AuthState.Unauthenticated) =>
      case _                                      => JsRouter.redirectTo(Page.Home)
  }
