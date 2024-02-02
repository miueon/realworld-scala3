package realworld.domain

import java.util.UUID

import cats.Functor
import cats.syntax.all.*

import realworld.domain.types.IsUUID
import realworld.effects.GenUUID

object ID:
  def make[F[_]: Functor: GenUUID, A: IsUUID]: F[A] =
    GenUUID[F].make.map(IsUUID[A].iso.get)

  def read[F[_]: Functor: GenUUID, A: IsUUID](str: String): F[A] =
    GenUUID[F].read(str).map(IsUUID[A].iso.get)

  extension (id: String)
    def toId[A: IsUUID]: A =
      IsUUID[A].iso.get(UUID.fromString(id))
