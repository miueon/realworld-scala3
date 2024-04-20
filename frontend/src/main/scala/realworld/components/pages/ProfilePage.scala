package realworld.components.pages

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import realworld.components.Component
import realworld.routes.Page

final case class ProfilePage(s_profile: Signal[Page.ProfilePage]) extends Component:
  
  def body: HtmlElement = ???
