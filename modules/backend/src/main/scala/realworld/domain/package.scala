package realworld

import doobie.util.meta.Meta
import realworld.spec.CreatedAt
import realworld.spec.UpdatedAt
import java.time.Instant
import smithy4s.Timestamp
import realworld.spec.Limit
import realworld.spec.Skip
import realworld.spec.CommentId
import doobie.util.meta.MetaConstructors
import doobie.postgres.JavaTimeInstances
import smithy4s.Newtype
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.circe
import io.circe.Codec
import io.circe.*


package object domain:
  object DoobieMeta extends MetaConstructors with JavaTimeInstances
  import DoobieMeta.given

  def metaOf[A: Meta](nt: Newtype[A]): Meta[nt.Type] = Meta[A].imap(nt.apply)(_.value)

  extension [A](meta: Meta[A])
    inline def refined[C](using inline constraint: Constraint[A, C]): Meta[A :| C] =
      meta.imap[A :| C](_.refine[C])(_.asInstanceOf[A])

  extension [A](c: Codec[A])
    inline def refined[C](using inline constraint: Constraint[A, C]): Codec[A :| C] =
      c.iemap[A :| C](_.refineEither[C])(_.asInstanceOf[A])

  inline given [A, C](using
      inline meta: Meta[A],
      inline constraint: Constraint[A, C]
  ): Meta[A :| C] =
    meta.refined

  given Meta[CreatedAt] =
    Meta[Instant].imap(i => CreatedAt(Timestamp.fromInstant(i)))(_.value.toInstant)
  given Meta[UpdatedAt] =
    Meta[Instant].imap(i => UpdatedAt(Timestamp.fromInstant(i)))(_.value.toInstant)

  given Meta[Limit]     = metaOf(Limit)
  given Meta[Skip]      = metaOf(Skip)
  given Meta[CommentId] = metaOf(CommentId)
end domain
