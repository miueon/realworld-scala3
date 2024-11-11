package realworld
package tests
package integration

import weaver.*
import cats.effect.IO
import cats.effect.kernel.Resource

object Resources extends GlobalResource:
  def sharedResources(global: GlobalWrite): Resource[IO, Unit] = 
    baseResource.flatMap(global.putR(_))

  def baseResource: Resource[IO, Probe] = Fixture.resource

  def sharedResourceOrFallback(read: GlobalRead): Resource[IO, Probe] = 
    read.getR[Probe]().flatMap{
      case Some(value) => Resource.eval(IO(value))
      case None => baseResource
    }

abstract class IntegrationSuite(global: GlobalRead) extends RealworldSuite:
  import Resources.*

  override def sharedResource = sharedResourceOrFallback(global)