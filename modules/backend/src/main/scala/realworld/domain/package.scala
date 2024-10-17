package realworld

import doobie.postgres.JavaTimeInstances
import doobie.util.meta.{Meta, MetaConstructors}
import io.github.iltotore.iron.*
import realworld.spec.{CommentId, CreatedAt, Limit, Skip, UpdatedAt}
import smithy4s.{Newtype, Timestamp}

import java.time.Instant

package object domain:
  object DoobieMeta extends MetaConstructors with JavaTimeInstances
  import DoobieMeta.given

  def metaOf[A: Meta](nt: Newtype[A]): Meta[nt.Type] = Meta[A].imap(nt.apply)(_.value)

  extension [A](meta: Meta[A])
    inline def refined[C](using inline constraint: Constraint[A, C]): Meta[A :| C] =
      meta.imap[A :| C](_.refine[C])(_.asInstanceOf[A])

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
  // given Meta[String].refined[Not[Blank]]
end domain
