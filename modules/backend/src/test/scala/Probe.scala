package realworld
package tests

import org.http4s.Uri
import realworld.config.types.AppConfig
import cats.effect.IO
import org.http4s.client.Client
import cats.effect.kernel.Resource

case class Probe(
  api: Api,
  serverUri: Uri,
  gen: Generator,
  config: AppConfig
)

object Probe:
  def make(
    client: Client[IO],
    uri: Uri,
    config: AppConfig,
  ) = 
    Resource.eval(
      for
        gen <- Generator.make
        api <- Api.make(client, uri)
      yield Probe(api, uri, gen, config)
    )