package realworld

import doobie.postgres.JavaTimeInstances
import doobie.util.meta.{Meta, MetaConstructors}
import io.github.iltotore.iron.*
import realworld.macroutil.*
import realworld.spec.{CommentId, CreatedAt, Limit, Skip, UpdatedAt}
import smithy4s.Timestamp

import java.time.Instant

package object domain:
  object DoobieMeta extends MetaConstructors with JavaTimeInstances
  import DoobieMeta.given

  extension [A](meta: Meta[A])
    inline def refined[C](using inline constraint: Constraint[A, C]): Meta[A :| C] =
      meta.imap[A :| C](_.refine[C])(_.asInstanceOf[A])

  inline given [A, C](
    using
    inline meta: Meta[A],
    inline constraint: Constraint[A, C]
  ): Meta[A :| C] =
    meta.refined

  given Meta[Timestamp] = Meta[Instant].imap(Timestamp.fromInstant)(_.toInstant)

  given Meta[CreatedAt] = deriveInstance
  given Meta[UpdatedAt] = deriveInstance
  given Meta[Limit]     = deriveInstance
  given Meta[Skip]      = deriveInstance
  given Meta[CommentId] = deriveInstance
end domain
