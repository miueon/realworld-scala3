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

object App:
  def renderPage(using state: AppState, api: Api): Signal[HtmlElement] =
    SplitRender(JsRouter.currentPageSignal.distinct)
      .collectStatic(Page.Home)(Home().body)
      .collectStatic(Page.Login)(Login().body)
      .collectStatic(Page.Register)(Register().body)
      .signal

  def main() =
    given Api      = Api.create()
    given AppState = AppState.init
    div(
      Header(),
      child <-- renderPage,
      Footer()
    )
end App
