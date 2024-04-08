package realworld
import com.raquo.laminar.api.L.{*, given}
import realworld.components.widgets.Header
import realworld.components.widgets.Footer
import realworld.components.pages.Home
import realworld.api.Api

object App:
  def main() =
    given Api = Api.create()
    given AppState = AppState.init
    div(
      Header(),
      Home(),
      Footer()
    )
