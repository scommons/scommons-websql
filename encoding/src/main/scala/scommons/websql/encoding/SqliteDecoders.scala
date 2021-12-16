package scommons.websql.encoding

import java.util.{Date, UUID}
import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.typedarray.Int8Array

/**
 * @see https://www.sqlite.org/datatype3.html
 */
trait SqliteDecoders extends BaseEncodingDsl {

  private def decoderUnsafe[T: ClassTag]: Decoder[T] =
    WebSqlDecoder((index: Index, row: ResultRow) => row[T](index))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    WebSqlDecoder { (index: Index, row: ResultRow) =>
      if (!row.isDefinedAt(index)) {
        row.skipIndices(1)
        None
      }
      else Some(d(index, row))
    }

  implicit val doubleToBoolean: MappedEncoding[Double, Boolean] = mappedEncoding[Double, Boolean](_ != 0.0)
  implicit val doubleToBigDecimal: MappedEncoding[Double, BigDecimal] = mappedEncoding[Double, BigDecimal](BigDecimal(_))
  implicit val doubleToByte: MappedEncoding[Double, Byte] = mappedEncoding[Double, Byte](_.toByte)
  implicit val doubleToShort: MappedEncoding[Double, Short] = mappedEncoding[Double, Short](_.toShort)
  implicit val doubleToInt: MappedEncoding[Double, Int] = mappedEncoding[Double, Int](_.toInt)
  implicit val doubleToLong: MappedEncoding[Double, Long] = mappedEncoding[Double, Long](_.toLong)
  implicit val doubleToFloat: MappedEncoding[Double, Float] = mappedEncoding[Double, Float](_.toFloat)
  implicit val stringToUUID: MappedEncoding[String, UUID] = mappedEncoding[String, UUID](UUID.fromString)

  implicit val int8ArrayToByteArray: MappedEncoding[Int8Array, Array[Byte]] =
    mappedEncoding[Int8Array, Array[Byte]](_.toArray)

  implicit val int8ArrayToByteSeq: MappedEncoding[Int8Array, Seq[Byte]] =
    mappedEncoding[Int8Array, Seq[Byte]](in => ArraySeq.unsafeWrapArray(in.toArray))

  implicit val jsDateToDate: MappedEncoding[js.Date, Date] = mappedEncoding[js.Date, Date](d => new Date(d.getTime().toLong))

  implicit val stringDecoder: Decoder[String] = decoderUnsafe[String]
  implicit val doubleDecoder: Decoder[Double] = decoderUnsafe[Double]

  implicit val int8ArrayDecoder: Decoder[Int8Array] =
    WebSqlDecoder { (index: Index, row: ResultRow) =>
      row[Any](index).asInstanceOf[Int8Array]
    }

  implicit val jsDateDecoder: Decoder[js.Date] =
    WebSqlDecoder { (index: Index, row: ResultRow) =>
      row[Any](index) match {
        case v: String =>
          val gmt = if (v.endsWith("Z") || v.contains("GMT")) v else s"$v GMT"
          new js.Date(gmt)
        case v: Double => new js.Date(v)
      }
    }

  implicit val booleanDecoder: Decoder[Boolean] = mappedDecoder[Double, Boolean]
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = mappedDecoder[Double, BigDecimal]
  implicit val byteDecoder: Decoder[Byte] = mappedDecoder[Double, Byte]
  implicit val shortDecoder: Decoder[Short] = mappedDecoder[Double, Short]
  implicit val intDecoder: Decoder[Int] = mappedDecoder[Double, Int]
  implicit val longDecoder: Decoder[Long] = mappedDecoder[Double, Long]
  implicit val floatDecoder: Decoder[Float] = mappedDecoder[Double, Float]
  implicit val uuidDecoder: Decoder[UUID] = mappedDecoder[String, UUID]
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = mappedDecoder[Int8Array, Array[Byte]]
  implicit val byteSeqDecoder: Decoder[Seq[Byte]] = mappedDecoder[Int8Array, Seq[Byte]]
  implicit val dateDecoder: Decoder[Date] = mappedDecoder[js.Date, Date]
}
