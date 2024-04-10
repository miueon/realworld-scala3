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

object App:
  extension [P](sp: SplitRender[P, HtmlElement])
    def collectComponent(p: P)(c: => Component) =
      sp.collectStatic(p)(c.body)

  def renderPage(using state: AppState, api: Api): Signal[HtmlElement] =
    SplitRender(JsRouter.currentPageSignal.distinct)
      .collectComponent(Page.Home)(Home())
      .collectComponent(Page.Login)(Login())
      .collectComponent(Page.Register)(Register())
      .signal

  def main() =
    given Api             = Api.create()
    given state: AppState = AppState.init
    div(
      onMountCallback(_ => state.events.emit(AuthEvent.Load)),
      Header(),
      child <-- renderPage,
      Footer(),
      AuthStateWatcher(state.events).loop
    )
end App
