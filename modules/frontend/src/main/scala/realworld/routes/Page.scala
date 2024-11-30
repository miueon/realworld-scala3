package realworld.routes

import io.github.iltotore.iron.upickle.given
import realworld.codec.given
import realworld.spec.Slug
import realworld.types.*
import upickle.default.*
sealed trait Page derives ReadWriter
object Page:
  case object Home                           extends Page
  case object Login                          extends Page
  case object Register                       extends Page
  case object Setting                        extends Page
  case object NewArticle                     extends Page
  case class ProfilePage(username: Username) extends Page
  case class ArticleDetailPage(slug: Slug) extends Page
  case class EditArticlePage(slug: Slug) extends Page
end Page
