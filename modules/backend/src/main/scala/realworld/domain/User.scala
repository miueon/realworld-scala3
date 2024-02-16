package realworld.domain.user

import java.util.UUID
import javax.crypto.Cipher

import cats.syntax.all.*

import doobie.*
import io.circe.Codec
import realworld.domain.given
import realworld.domain.types.DeriveType
import realworld.domain.types.IdNewtype
import realworld.domain.types.IsUUID
import realworld.domain.types.Newtype
import realworld.domain.types.Wrapper
import realworld.spec.Bio
import realworld.spec.Email
import realworld.spec.ImageUrl
import realworld.spec.Token
import realworld.spec.User
import realworld.spec.Username
import scala.CanEqual.derived
import realworld.domain.WithId
import scala.util.control.NoStackTrace
import realworld.spec.Profile
import realworld.spec.Author

type UserId = UserId.Type
object UserId extends IdNewtype

type EncryptedPassword = EncryptedPassword.Type
object EncryptedPassword extends Newtype[String]

case class EncryptCipher(value: Cipher)
case class DecryptCipher(value: Cipher)

given Meta[Email]    = Meta[String].imap(Email(_))(_.value)
given Meta[Username] = Meta[String].imap(Username(_))(_.value)
given Meta[EncryptedPassword] =
  Meta[String].imap(EncryptedPassword(_))(_.value)
given Meta[Bio]      = Meta[String].imap(Bio(_))(_.value)
given Meta[ImageUrl] = Meta[String].imap(ImageUrl(_))(_.value)

case class DBUser(
    email: Email,
    username: Username,
    password: EncryptedPassword,
    bio: Option[Bio] = None,
    image: Option[ImageUrl] = None
) derives Codec.AsObject:
  def toUser(tokenOpt: Option[Token]) =
    User(
      email,
      username,
      tokenOpt,
      bio,
      image
    )

  def toProfiile(following: Boolean): Profile =
    Profile(username, following, bio, image)

  def toAuthor(following: Boolean): Author =
    Author(username, following, bio, image)
end DBUser
object Users extends TableDefinition("users"):
  val id: Column[UserId]                  = Column("id")
  val email: Column[Email]                = Column("email")
  val username: Column[Username]          = Column("username")
  val password: Column[EncryptedPassword] = Column("password")
  val bio: Column[Option[Bio]]            = Column("bio")
  val image: Column[Option[ImageUrl]]     = Column("image")

  object UserSqlDef
      extends WithSQLDefinition[DBUser](
        Composite(
          (
            email.sqlDef,
            username.sqlDef,
            password.sqlDef,
            bio.sqlDef,
            image.sqlDef
          )
        )(DBUser.apply)(Tuple.fromProductTyped)
      )
  val columns = UserSqlDef
  val rowCol  = WithId.sqlDef(using id, columns)
end Users

enum UserError extends NoStackTrace:
  case UserNotFound()
  case UserPasswordNotMatched()
  case ProfileNotFound()
  case EmailAlreadyExists()
  case UsernameAlreadyExists()
  case UserFollowingHimself(profile: Profile)
  case UserUnfollowingHimself(profile: Profile)
