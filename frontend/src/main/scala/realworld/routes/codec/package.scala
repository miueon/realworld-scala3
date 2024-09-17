package realworld

import realworld.spec.Bio
import realworld.spec.Email
import realworld.spec.ImageUrl
import realworld.spec.Slug
import realworld.spec.Token
import realworld.spec.User
import realworld.spec.Username
import smithy4s.Document
import ujson.Arr
import upickle.core.*
import upickle.default.*
package object codec:
  def documentToJson(doc: Document): ujson.Value = doc match
    case Document.DNumber(value)  => ujson.Num(value.toDouble)
    case Document.DString(value)  => ujson.Str(value)
    case Document.DBoolean(value) => ujson.Bool(value)
    case Document.DNull           => ujson.Null
    case Document.DArray(value)   => ujson.Arr(value.map(documentToJson))
    case Document.DObject(value)  => ujson.Obj.from(value.mapValues(documentToJson).toSeq)

  def jsonToDocument(json: ujson.Value): Document = 
    json match
      case Arr(arr) => Document.DArray(arr.map(jsonToDocument).toIndexedSeq)
      case ujson.Null => Document.DNull
      case ujson.False => Document.DBoolean(false)
      case ujson.True => Document.DBoolean(true)
      case ujson.Num(num) => Document.DNumber(BigDecimal(num))
      case ujson.Str(str) => Document.DString(str)
      case ujson.Obj(obj) => Document.DObject(obj.toMap.mapValues(jsonToDocument).toMap)

  def documentCodec[A](implicit
      docEncoder: smithy4s.Document.Encoder[A],
      docDecoder: smithy4s.Document.Decoder[A]
  ): ReadWriter[A] =
    val encoder: Writer[A] = writer[ujson.Value].comap(a => documentToJson(docEncoder.encode(a)))

    val decoder: Reader[A] = 
      reader[ujson.Value].map(value => (jsonToDocument andThen docDecoder.decode)(value).fold(err => throw new upickle.core.Abort(s"Failed to decode document: $err"), identity))

    ReadWriter.join[A](decoder, encoder)
  end documentCodec

  given ReadWriter[User]     = documentCodec
  given ReadWriter[Email]    = documentCodec
  given ReadWriter[Username] = documentCodec
  given ReadWriter[Token]    = documentCodec
  given ReadWriter[Bio]      = documentCodec
  given ReadWriter[ImageUrl] = documentCodec
  given ReadWriter[Slug]     = documentCodec
end codec
