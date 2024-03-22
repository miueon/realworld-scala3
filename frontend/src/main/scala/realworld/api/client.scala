package realworld.api

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.raquo.airstream.core.EventStream as LaminarStream
import org.http4s.Uri
import org.http4s.dom.FetchClientBuilder
import org.scalajs.dom
import realworld.spec.ArticleService
import realworld.spec.CommentService
import realworld.spec.TagService
import realworld.spec.UserService
import smithy4s.Service
import smithy4s.http4s.*

import scala.concurrent.Future
class Api private (
    val articles: ArticleService[IO],
    val tags: TagService[IO],
    val users: UserService[IO],
    val comments: CommentService[IO]
):
  import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.*
  def future[A](a: Api => IO[A]): Future[A] =
    a(this).unsafeToFuture()

  def stream[A](a: Api => IO[A]): LaminarStream[A] =
    LaminarStream.fromFuture(future(a))
end Api

object Api:
  def create(location: String = dom.window.location.origin.get) =
    val uri = Uri.unsafeFromString(location)

    val client = FetchClientBuilder[IO].create

    def simpleServiceGen[Alg[_[_, _, _, _, _]]](service: Service[Alg]) =
      SimpleRestJsonBuilder(service)
        .client(client)
        .uri(uri)
        .make
        .fold(throw _, identity)

    val articles = simpleServiceGen(ArticleService)
    val tags     = simpleServiceGen(TagService)
    val users    = simpleServiceGen(UserService)
    val comments = simpleServiceGen(CommentService)

    Api(articles, tags, users, comments)
  end create
end Api
