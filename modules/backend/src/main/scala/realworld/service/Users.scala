package realworld.service

import cats.effect.*
import cats.syntax.all.*

import doobie.*
import doobie.implicits.*
import realworld.domain.users.UserId
import realworld.spec.Email
import realworld.spec.NotFoundError
import realworld.spec.User
import realworld.domain.users.UserWithPassword
import doobie.hikari.HikariTransactor
import realworld.db.Database
import realworld.spec.RegisterUserData
import realworld.spec.Username
import realworld.domain.users.EncryptedPassword
import cats.data.NonEmptyVector
import realworld.effects.GenUUID
import realworld.domain.ID

trait Users[F[_]]:
  def get(userId: UserId): F[User]
  def findByEmail(email: Email): F[Option[UserWithPassword]]
  def create(
      user: RegisterUserData,
      encryptedPassword: EncryptedPassword
  ): F[Int]

object Users:
  def make[F[_]: MonadCancelThrow: GenUUID](db: Database[F]) =
    new Users[F]:
      import UserSQL as u
      def get(userId: UserId): F[User] =
        db.transact(u.get(userId))
          .flatMap:
            case Some(user) => user.pure[F]
            case None       => NotFoundError().raiseError[F, User]

      def findByEmail(email: Email): F[Option[UserWithPassword]] =
        db.transact(u.findByEmail(email))
      def create(
          user: RegisterUserData,
          encryptedPassword: EncryptedPassword
      ): F[Int] =
        ID.make[F, UserId]
          .flatMap(userId =>
            db.transact(
              u.create(userId, user.username, user.email, encryptedPassword)
            )
          )
end Users

private object UserSQL:
  import realworld.domain.users.Users as u
  import realworld.domain.users.UserWithPassword as upass
  import realworld.domain.users.given

  private def queryUser(
      conditionFragment: Fragment
  ): ConnectionIO[Option[User]] =
    (fr"SELECT ${u.columns} FROM $u" ++ conditionFragment)
      .queryOf(u.columns)
      .option

  def get(userId: UserId): ConnectionIO[Option[User]] =
    queryUser(fr"WHERE ${u.id === userId}")

  def findByEmail(email: Email): ConnectionIO[Option[UserWithPassword]] =
    sql"SELECT ${upass.columns} FROM $u WHERE ${u.email === email}"
      .queryOf(upass.columns)
      .option

  def create(
      id: UserId,
      username: Username,
      email: Email,
      password: EncryptedPassword
  ): ConnectionIO[Int] =
    insertInto(
      u,
      NonEmptyVector.of(
        u.id --> id,
        u.username --> username,
        u.email --> email,
        u.password --> password
      )
    ).update.run

end UserSQL
