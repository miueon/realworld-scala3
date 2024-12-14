package realworld.api

import com.raquo.airstream.core.EventStream as LaminarStream
import org.scalajs.dom
import realworld.spec.{ArticleService, CommentService, TagService, UserService}
import smithy4s.Service
import smithy4s_fetch.*

import scala.concurrent.Future
import scala.scalajs.js.Promise
export scala.scalajs.js.Thenable.Implicits.thenable2future

class Api private (
  val articlePromise: ArticleService[Promise],
  val tagPromise: TagService[Promise],
  val userPromise: UserService[Promise],
  val commentPromise: CommentService[Promise]
):
  import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.*
  def promise[A](a: Api => Future[A]): Future[A] =
    a(this)

  def promiseStream[A](a: Api => Future[A]): LaminarStream[A] =
    LaminarStream.fromFuture(a(this))
end Api

object Api:
  def create(location: String = dom.window.location.origin) =

    def simpleRestJsonFetchGen[Alg[_[_, _, _, _, _]]](service: Service[Alg]) =
      SimpleRestJsonFetchClient(
        service,
        location
      ).make

    val articlePromise = simpleRestJsonFetchGen(ArticleService)
    val tagPromise     = simpleRestJsonFetchGen(TagService)
    val userPromise    = simpleRestJsonFetchGen(UserService)
    val commentPromise = simpleRestJsonFetchGen(CommentService)

    Api(articlePromise, tagPromise, userPromise, commentPromise)
  end create
end Api
