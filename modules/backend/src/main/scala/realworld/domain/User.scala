package realworld.domain.user

import doobie.*
import io.circe.*
import io.circe.Decoder.Result
import io.github.iltotore.iron.*
import io.github.iltotore.iron.circe.given
import io.github.iltotore.iron.constraint.all.*
import io.github.iltotore.iron.doobie.given
import javax.crypto.Cipher
import realworld.codec.given
import realworld.domain.*
import realworld.domain.types.{IdNewtype, Newtype}
import realworld.macroutil.*
import realworld.spec.{Bio, Profile}
import realworld.types.{Email, EmailConstraint, ImageUrl, Username, UsernameConstraint}

import scala.util.control.NoStackTrace

type UserId = UserId.Type
object UserId extends IdNewtype

type EncryptedPassword = EncryptedPassword.Type
object EncryptedPassword extends Newtype[String]

case class EncryptCipher(value: Cipher)
case class DecryptCipher(value: Cipher)

// given Meta[Email]             = Meta[String].refined[EmailConstraint]
// given Meta[Username]          = Meta[String].refined[UsernameConstraint]
given Meta[EncryptedPassword] = EncryptedPassword.derive
given Meta[Bio]               = deriveInstance
// given Meta[Bio]               = metaOf(Bio)

case class DBUser(
  email: Email,
  username: Username,
  password: EncryptedPassword,
  bio: Option[Bio] = None,
  image: Option[ImageUrl] = None
) derives Codec.AsObject

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
  val rowCol  = WithId.sqlDef(using id, columns, this)
end Users

enum UserError extends NoStackTrace:
  def msg: String
  case UserNotFound(msg: String = "User not found")                   extends UserError
  case UserPasswordNotMatched(msg: String = "Password not matched")   extends UserError
  case ProfileNotFound(msg: String = "Profile not found")             extends UserError
  case EmailAlreadyExists(msg: String = "Email already exists")       extends UserError
  case UsernameAlreadyExists(msg: String = "Username already exists") extends UserError
  case UserFollowingHimself(msg: String = "User should not follow himself", profile: Profile)
  extends UserError
  case UserUnfollowingHimself(msg: String = "User is unfollowing himself", profile: Profile)
  extends UserError
