package jobby
package app

import cats.effect.*
import cats.syntax.all.*

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import java.io.File
import java.io.FileInputStream
import jobby.Routes


object Main extends IOApp:
  import scala.jdk.CollectionConverters.*

  def run(args: List[String]) =

    val serverResource =
      for 
        routes <- Routes.all
        server <- Server(routes)
      yield server

    serverResource.use(_ => IO.never) 
  end run
end Main
