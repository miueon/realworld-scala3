package realworld
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.Router
import realworld.routes.Page

object App:
  def main() =
    given Router[Page] = Page.router
    div(

    )
