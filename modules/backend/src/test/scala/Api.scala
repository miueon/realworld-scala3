package realworld

package tests

import realworld.spec.ArticleService
import cats.effect.*
import realworld.spec.CommentService
import realworld.spec.TagService
import realworld.spec.UserService
import org.http4s.client.Client
import org.http4s.Uri
import smithy4s.http4s.SimpleRestJsonBuilder
import cats.syntax.all.*

case class Api(
    articles: ArticleService[IO],
    comments: CommentService[IO],
    tags: TagService[IO],
    users: UserService[IO]
)

object Api:
  def make(client: Client[IO], uri: Uri): IO[Api] =
    val articles = IO.fromEither(
      SimpleRestJsonBuilder(ArticleService)
        .client(client)
        .uri(uri)
        .make
    )

    val comments = IO.fromEither(
      SimpleRestJsonBuilder(CommentService)
        .client(client)
        .uri(uri)
        .make
    )

    val tags = IO.fromEither(
      SimpleRestJsonBuilder(TagService)
        .client(client)
        .uri(uri)
        .make
    )
    val users = IO.fromEither(
      SimpleRestJsonBuilder(UserService)
        .client(client)
        .uri(uri)
        .make
    )

    (articles, comments, tags, users).mapN(Api.apply)
  end make
end Api
