package realworld
package tests

import org.http4s.Uri
import realworld.config.types.AppConfig
import cats.effect.IO
import org.http4s.client.Client
import cats.effect.kernel.Resource
import cats.effect.kernel.Ref
import weaver.Log.Entry
import cats.data.Chain

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
