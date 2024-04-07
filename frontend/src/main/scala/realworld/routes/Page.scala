package realworld.routes

import com.raquo.laminar.api.L.*
import com.raquo.laminar.api.L
import io.circe.*
import realworld.spec.Username
import realworld.codec.given
import realworld.spec.Slug

sealed trait Page derives Codec.AsObject
object Page:
  case object Home                       extends Page
  case object Login                      extends Page
  case object Register                   extends Page
  case object Setting                    extends Page
  case object Editor                     extends Page
  case class ProfilePage(username: Username) extends Page
  case class ArticlePage(slug: Slug)         extends Page
  case class EditArticlePage(slug: Slug)     extends Page
end Page
