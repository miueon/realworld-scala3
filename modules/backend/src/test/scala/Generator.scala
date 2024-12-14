package realworld
package tests

import cats.effect.IO
import cats.effect.std.Random
import cats.syntax.all.*
import io.github.iltotore.iron.RefinedTypeOps
import realworld.domain.types.IdNewtype
import realworld.effects.GenUUID
import realworld.types.Email

case class Generator private (random: Random[IO], uuid: GenUUID[IO]):
  def id(nt: IdNewtype): IO[nt.Type] =
    uuid.make.map(nt.apply)

  def int(nt: smithy4s.Newtype[Int], min: Int, max: Int): IO[nt.Type] =
    random.betweenInt(min, max).map(nt.apply)

  def str(toNewType: smithy4s.Newtype[String], lengthRange: Range = 0 to 100): IO[toNewType.Type] =
    for
      length <- random.betweenInt(lengthRange.start, lengthRange.end)
      chars  <- random.nextAlphaNumeric.replicateA(length).map(_.mkString)
      str = toNewType.getClass.getSimpleName.toString + "-" + chars
    yield toNewType(str.take(lengthRange.end))

  def strI[C, T](ops: RefinedTypeOps[String, C, T], lengthRange: Range = 10 to 42): IO[T] =
    for
      length <- random.betweenInt(lengthRange.start, lengthRange.end)
      chars  <- random.nextAlphaNumeric.replicateA(length).map(_.mkString)
    yield ops.applyUnsafe(chars.take(lengthRange.end))

  def email: IO[Email] =
    for
      chars  <- random.nextAlphaNumeric.replicateA(5).map(_.mkString)
      domain <- random.nextAlphaNumeric.replicateA(5).map(_.mkString)
    yield Email.applyUnsafe(s"${chars}@${domain}.com")

  def url(nt: smithy4s.Newtype[String]): IO[nt.Type] =
    str(nt, 0 to 100).map { v =>
      nt.apply(s"https://${v.value}.com")
    }
end Generator

object Generator:
  def make: IO[Generator] =
    (Random.scalaUtilRandom[IO], GenUUID.forSync[IO].pure[IO]).mapN(Generator.apply)
