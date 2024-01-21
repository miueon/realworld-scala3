import realworld.spec.HelloWorldService
import realworld.spec.HelloWorldResponse
import realworld.spec.HelloWorldServiceGen
import cats.effect.IO
import cats.syntax.all.*
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.HttpRoutes
import cats.effect.kernel.Resource
import cats.effect.IOApp

object HelloWorldImpl extends HelloWorldService[IO]:
  def get(): IO[HelloWorldResponse] = IO.pure:
    HelloWorldResponse("Hello")

object Routes:
  private val example = SimpleRestJsonBuilder.routes(HelloWorldImpl).resource

  private val docs = smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)


