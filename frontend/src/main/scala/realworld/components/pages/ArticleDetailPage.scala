package realworld.components.pages

import realworld.components.Component
import com.raquo.laminar.api.L.*
import realworld.AppState
import realworld.api.Api
import realworld.routes.Page

final case class ArticleDetailPage(s_page: Signal[Page.ArticleDetailPage])(using
    state: AppState,
    api: Api
) extends Component:

  def body: HtmlElement = ???
