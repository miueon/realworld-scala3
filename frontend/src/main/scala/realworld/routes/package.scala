package realworld

import com.raquo.waypoint.*
import urldsl.language.PathSegment
import urldsl.errors.DummyError
import realworld.routes.Page.ProfilePage
import realworld.spec.Username
import realworld.routes.Page.ArticleDetailPage
import realworld.spec.Slug
import realworld.routes.Page.EditArticlePage
package object routes:
  def fragmentStatic[Page](staticPage: Page, pattern: PathSegment[Unit, DummyError]) =
    Route.static(staticPage, pattern, Route.fragmentBasePath)

  val profileRoute = Route(
    encode = (stp: ProfilePage) => stp.username.value,
    decode = (arg: String) => ProfilePage(Username(arg)),
    pattern = root / "profile" / segment[String],
    Route.fragmentBasePath
  )

  val articleDetailPageRoute = Route(
    encode = (stp: ArticleDetailPage) => stp.slug.value,
    decode = (arg: String) => ArticleDetailPage(Slug(arg)),
    pattern = root / "article" / segment[String] / endOfSegments,
    Route.fragmentBasePath
  )

  val editArticlePageRoute = Route(
    encode = (stp: EditArticlePage) => stp.slug.value,
    decode = (arg: String) => EditArticlePage(Slug(arg)),
    pattern = root / "editor" / segment[String] / endOfSegments,
    Route.fragmentBasePath
  )

  val routes = List(
    fragmentStatic(Page.Home, root / endOfSegments),
    fragmentStatic(Page.Login, root / "login"),
    fragmentStatic(Page.Register, root / "register"),
    fragmentStatic(Page.Setting, root / "settings"),
    fragmentStatic(Page.NewArticle, root / "editor"),
    articleDetailPageRoute,
    profileRoute,
    editArticlePageRoute
  )
end routes
