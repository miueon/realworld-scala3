package realworld

package tests

import cats.effect.*
import cats.syntax.all.*
import org.http4s.Uri
import org.http4s.client.Client
import realworld.spec.{ArticleService, CommentService, TagService, UserService}
import smithy4s.http4s.SimpleRestJsonBuilder

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
