package realworld

import cats.syntax.all.*

import io.circe.Codec
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.Json
import io.circe.syntax.*
import realworld.spec.User
import smithy4s.Document
import realworld.spec.Email
import realworld.spec.Username
import realworld.spec.Token
import realworld.spec.Bio
import realworld.spec.ImageUrl
import doobie.util.meta.Meta
import realworld.spec.CreatedAt
import realworld.spec.UpdatedAt
import java.time.Instant
import smithy4s.Timestamp
import realworld.spec.Limit
import realworld.spec.Skip
import realworld.spec.CommentId
import doobie.util.meta.MetaConstructors
import doobie.postgres.JavaTimeInstances

package object domain:
  def documentToJson(doc: Document): Json = doc match
    case Document.DNumber(value)  => value.asJson
    case Document.DString(value)  => value.asJson
    case Document.DBoolean(value) => value.asJson
    case Document.DNull           => Json.Null
    case Document.DArray(value)   => value.map(documentToJson).asJson
    case Document.DObject(value)  => value.mapValues(documentToJson).toMap.asJson

  def jsonToDocument(json: Json): Document = json.fold(
    Document.DNull,
    bool => Document.DBoolean(bool),
    num => Document.DNumber(num.toBigDecimal.getOrElse(BigDecimal(0))),
    str => Document.DString(str),
    arr => Document.DArray(arr.map(jsonToDocument).toIndexedSeq),
    obj => Document.DObject(obj.toMap.mapValues(jsonToDocument).toMap)
  )

  def documentCodec[A](implicit
      docEncoder: smithy4s.Document.Encoder[A],
      docDecoder: smithy4s.Document.Decoder[A]
  ): Codec[A] =
    val encoder: Encoder[A] = Encoder.instance { a =>
      documentToJson(docEncoder.encode(a))
    }

    val decoder: Decoder[A] = Decoder.instance { hCursor =>
      hCursor
        .as[Json]
        .map(jsonToDocument)
        .flatMap(
          docDecoder
            .decode(_)
            .leftMap(err => DecodingFailure(err.toString, hCursor.history))
        )
    }

    Codec.from(decoder, encoder)
  end documentCodec

  given Codec[User]     = documentCodec
  given Codec[Email]    = documentCodec
  given Codec[Username] = documentCodec
  given Codec[Token]    = documentCodec
  given Codec[Bio]      = documentCodec
  given Codec[ImageUrl] = documentCodec

  object DoobieMeta extends MetaConstructors with JavaTimeInstances 
  import DoobieMeta.given

  given Meta[CreatedAt] =
    Meta[Instant].imap(i => CreatedAt(Timestamp.fromInstant(i)))(_.value.toInstant)
  given Meta[UpdatedAt] =
    Meta[Instant].imap(i => UpdatedAt(Timestamp.fromInstant(i)))(_.value.toInstant)

  given Meta[Limit]     = Meta[Int].imap(Limit(_))(_.value)
  given Meta[Skip]      = Meta[Int].imap(Skip(_))(_.value)
  given Meta[CommentId] = Meta[Int].imap(CommentId(_))(_.value)
end domain
