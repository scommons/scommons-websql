package scommons.websql.quill

import java.time.LocalDate
import java.util.{Date, UUID}

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag
import scala.scalajs.js
import scala.scalajs.js.typedarray.Int8Array

/**
  * @see https://www.sqlite.org/datatype3.html
  */
trait SqliteDecoders {
  this: SqliteContext[_] =>

  type Decoder[T] = SqliteDecoder[T]

  case class SqliteDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    def apply(index: Index, row: ResultRow): T = decoder(index, row)
  }

  private def decoderUnsafe[T: ClassTag]: Decoder[T] =
    SqliteDecoder((index: Index, row: ResultRow) => row[T](index))

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    SqliteDecoder((index: Index, row: ResultRow) => mapped.f(d.apply(index, row)))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    SqliteDecoder { (index: Index, row: ResultRow) =>
      val v = row.data(index)
      if (v == null || js.isUndefined(v)) None
      else Some(d.apply(index, row))
    }

  implicit val doubleToBoolean: MappedEncoding[Double, Boolean] = MappedEncoding(_ != 0.0)
  implicit val doubleToBigDecimal: MappedEncoding[Double, BigDecimal] = MappedEncoding(BigDecimal(_))
  implicit val doubleToByte: MappedEncoding[Double, Byte] = MappedEncoding(_.toByte)
  implicit val doubleToShort: MappedEncoding[Double, Short] = MappedEncoding(_.toShort)
  implicit val doubleToInt: MappedEncoding[Double, Int] = MappedEncoding(_.toInt)
  implicit val doubleToLong: MappedEncoding[Double, Long] = MappedEncoding(_.toLong)
  implicit val doubleToFloat: MappedEncoding[Double, Float] = MappedEncoding(_.toFloat)
  implicit val stringToUUID: MappedEncoding[String, UUID] = MappedEncoding(UUID.fromString)
  implicit val int8ArrayToByteArray: MappedEncoding[Int8Array, Array[Byte]] = MappedEncoding(_.toArray)
  implicit val int8ArrayToByteSeq: MappedEncoding[Int8Array, Seq[Byte]] = MappedEncoding { in =>
    ArraySeq.unsafeWrapArray(in.toArray)
  }
  implicit val jsDateToDate: MappedEncoding[js.Date, Date] = MappedEncoding(d => new Date(d.getTime().toLong))
  implicit val jsDateToLocalDate: MappedEncoding[js.Date, LocalDate] = MappedEncoding(d =>
    LocalDate.of(d.getFullYear, d.getMonth + 1, d.getDate)
  )
  
  implicit val stringDecoder: Decoder[String] = decoderUnsafe[String]
  implicit val doubleDecoder: Decoder[Double] = decoderUnsafe[Double]
  
  implicit val int8ArrayDecoder: Decoder[Int8Array] =
    SqliteDecoder { (index: Index, row: ResultRow) =>
      row[Any](index).asInstanceOf[Int8Array]
    }
  
  implicit val jsDateDecoder: Decoder[js.Date] =
    SqliteDecoder { (index: Index, row: ResultRow) =>
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
  implicit val localDateDecoder: Decoder[LocalDate] = mappedDecoder[js.Date, LocalDate]
}
