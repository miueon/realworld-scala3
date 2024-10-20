package realworld

import com.raquo.waypoint.*
import io.github.iltotore.iron.*
import realworld.routes.Page.{ArticleDetailPage, EditArticlePage, ProfilePage}
import realworld.spec.Slug
import urldsl.errors.DummyError
import urldsl.language.PathSegment
package object routes:
  def fragmentStatic[Page](staticPage: Page, pattern: PathSegment[Unit, DummyError]) =
    Route.static(staticPage, pattern, Route.fragmentBasePath)

  val profileRoute = Route(
    encode = (stp: ProfilePage) => stp.username,
    decode = (arg: String) => ProfilePage(arg.assume),
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
