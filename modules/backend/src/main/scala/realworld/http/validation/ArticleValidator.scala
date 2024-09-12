package realworld.http.validation

import cats.syntax.all.*
import realworld.http.validation.validators.*
import realworld.spec.Body
import realworld.spec.Description
import realworld.spec.TagName
import realworld.spec.Title

case class InvalidTitle(errors: List[String], field: String = "title") extends InvalidField
case class InvalidDescription(errors: List[String], field: String = "description")
    extends InvalidField
case class InvalidBody(errors: List[String], field: String = "body") extends InvalidField

def validTitle(title: Title): ValidationResult[Title] =
  val trimmed = title.value.trim()
  notBlank(trimmed)
    .leftMap(toInvalidField(_, InvalidTitle(_)))
    .map(Title(_))

def validDescription(description: Description): ValidationResult[Description] =
  val trimmed = description.value.trim()
  notBlank(trimmed).leftMap(toInvalidField(_, InvalidDescription(_))).map(Description(_))

def validBody(body: Body): ValidationResult[Body] =
  val trimmed = body.value.trim()
  notBlank(trimmed).leftMap(toInvalidField(_, InvalidBody(_))).map(Body(_))

def validTags(tags: List[TagName]): ValidationResult[List[TagName]] =
  tags.map(_.value.trim()).filter(_.nonEmpty).distinct.map(TagName(_)).validNec
