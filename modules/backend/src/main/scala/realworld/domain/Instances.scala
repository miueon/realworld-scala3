package realworld.domain

import java.util.UUID

import doobie.util.Read
import doobie.util.Write
import doobie.util.meta.Meta
import io.github.iltotore.iron.refine
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.CompilationCache
import smithy4s.schema.Schema

export Instances.given

object Instances:
  given Meta[UUID] = Meta[String].imap(UUID.fromString)(_.toString)

