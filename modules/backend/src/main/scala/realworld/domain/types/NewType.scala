package realworld.domain.types

import smithy4s.Newtype
import monocle.Iso
import cats.kernel.Eq
import cats.kernel.Order
import cats.Show
import doobie.util.Write
import doobie.util.Read
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
    eqv: Eq[A],
    ord: Order[A],
    shw: Show[A],
    wr: Write[A],
    rd: Read[A]
) extends DeriveType[A]:

  given Eq[Type]       = derive[Eq]
  given Order[Type]    = derive
  given Show[Type]     = derive
  given Write[Type]    = derive
  given Read[Type]     = derive
  given Ordering[Type] = derive(using ord.toOrdering)
end Newtype

import realworld.domain.Instances.given_Meta_UUID
abstract class IdNewtype extends Newtype[UUID]:
  given IsUUID[Type] = derive[IsUUID]
