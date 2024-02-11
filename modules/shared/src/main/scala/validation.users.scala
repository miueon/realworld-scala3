package realworld
package validation

import realworld.spec.*

private[validation] def err[T](msg: String) =
  Left[ValidationError, T](ValidationError(msg))

private[validation] def ok =
  Right[ValidationError, Unit](())

def validateUserName(login: Username) =
  val str = login.value.trim
  if str.length < 5 || str.length > 50 then
    err("Username cannot be shorter than 5, or longer than 50 characters")
  else ok

def validateUserPassword(password: Password) =
  val str = password.value
  if str.exists(_.isWhitespace) then
    err("Password cannot contain whitespace characters")
  else if str.length < 12 || str.length > 128 then
    err("Password cannot be shorter than 12 or longer than 128 characters")
  else ok
