package realworld

import cats.syntax.all.*
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}
import io.circe.syntax.*
import realworld.spec.{Bio, Email, ImageUrl, Slug, Token, User, Username}
import smithy4s.Document

package object codec:
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

  def documentCodec[A](
    implicit docEncoder: smithy4s.Document.Encoder[A],
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
  given Codec[Slug]     = documentCodec
end codec
