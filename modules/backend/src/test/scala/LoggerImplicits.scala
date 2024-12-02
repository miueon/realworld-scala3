package weaver

import org.typelevel.log4cats.{Logger => CatsLogger}
import weaver.{Log => WeaverLog}
import cats.Monad
import cats.syntax.all.*

object LoggerImplicits {
  implicit def loggerForWeaver[F[_]: Monad](implicit wl: WeaverLog[F]): CatsLogger[F] = new CatsLogger[F] {
    override def error(t: Throwable)(message: => String): F[Unit] = t.printStackTrace().pure[F] >> wl.error(msg = message, cause = t)

    override def warn(t: Throwable)(message: =>String): F[Unit] = t.printStackTrace().pure[F] >> wl.warn(msg = message, cause = t)

    override def info(t: Throwable)(message: =>String): F[Unit] = t.printStackTrace().pure[F] >> wl.info(msg = message, cause = t)

    override def debug(t: Throwable)(message: =>String): F[Unit] = t.printStackTrace().pure[F] >> wl.debug(msg = message, cause = t)

    override def trace(t: Throwable)(message: =>String): F[Unit] = t.printStackTrace().pure[F] >> wl.debug(msg = message, cause = t)

    override def error(message: =>String): F[Unit] = wl.error(message)

    override def warn(message: =>String): F[Unit] = wl.warn(message)

    override def info(message: =>String): F[Unit] = wl.info(message)

    override def debug(message: =>String): F[Unit] = wl.debug(message)

    override def trace(message: =>String): F[Unit] = wl.debug(message)
  }
}