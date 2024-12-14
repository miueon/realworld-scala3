package realworld
package tests

import cats.data.Chain
import cats.effect.IO
import cats.effect.kernel.{Ref, Resource}
import org.http4s.Uri
import org.http4s.client.Client
import realworld.config.types.AppConfig
import weaver.Log.Entry

case class Probe(
  api: Api,
  serverUri: Uri,
  gen: Generator,
  config: AppConfig,
  logs: Ref[IO, Chain[Entry]]
):
  def userDataSupport    = UserDataSupport(this)
  def articleDataSupport = ArticleDataSupport(this)

object Probe:
  def make(
    client: Client[IO],
    uri: Uri,
    config: AppConfig,
    logs: Ref[IO, Chain[Entry]]
  ) =
    Resource.eval(
      for
        gen <- Generator.make
        api <- Api.make(client, uri)
      yield Probe(api, uri, gen, config, logs)
    )
