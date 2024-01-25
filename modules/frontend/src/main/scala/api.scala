package jobby
import scala.concurrent.Future

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import org.scalajs.dom
import com.raquo.airstream.core.EventStream as LaminarStream
import realworld.spec.HelloWorldService
import org.http4s.Uri
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.dom.FetchClientBuilder

class Api private (
    val hello: HelloWorldService[IO]
):
  import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.*
  def future[A](a: Api => IO[A]): Future[A] =
    a(this).unsafeToFuture()

  def stream[A](a: Api => IO[A]): LaminarStream[A] =
    LaminarStream.fromFuture(future(a))

object Api:
  def create(location: String = dom.window.location.origin.get) =
    val uri = Uri.unsafeFromString(location)

    val client = FetchClientBuilder[IO].create

    val hello = SimpleRestJsonBuilder(HelloWorldService)
      .client(client)
      .uri(uri)
      .use
      .fold(throw _, identity)

    Api(hello)
