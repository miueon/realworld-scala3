package realworld.domain.types

import cats.Show
import cats.kernel.{Eq, Order}
import doobie.util.{Read, Write}
import io.circe.{Decoder, Encoder}
import monocle.Iso

import java.util.UUID

abstract class DeriveType[A]:
  opaque type Type = A
  inline def apply(a: A): Type                           = a
  inline final def derive[F[_]](using ev: F[A]): F[Type] = ev
  extension (t: Type) inline def value: A                = t
  extension (t: A) inline def asType: Type               = t
  given Wrapper[A, Type] with
    def iso: Iso[A, Type] =
      Iso[A, Type](apply(_))(_.value)

abstract class Newtype[A](using
    ord: Order[A],
    shw: Show[A],
    wr: Write[A],
    rd: Read[A],
    enc: Encoder[A],
    dec: Decoder[A]
) extends DeriveType[A]:

  given Eq[Type]       = derive[Eq]
  given Order[Type]    = derive
  given Show[Type]     = derive
  given Write[Type]    = derive
  given Read[Type]     = derive
  given Ordering[Type] = derive(using ord.toOrdering)
  given Encoder[Type]  = derive
  given Decoder[Type]  = derive
end Newtype

import doobie.postgres.implicits.*
abstract class IdNewtype extends Newtype[UUID]:
  given IsUUID[Type] = derive[IsUUID]
