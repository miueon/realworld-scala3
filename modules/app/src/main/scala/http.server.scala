package jobby

import org.http4s.ember.server.EmberServerBuilder
import cats.effect.IO
import org.http4s.HttpApp
import com.comcast.ip4s.*

def Server(app: HttpApp[IO]) =
  EmberServerBuilder
    .default[IO]
    .withPort(port"8080")
    .withHost(host"127.0.0.1")
    .withHttpApp(app)
    .build
end Server
