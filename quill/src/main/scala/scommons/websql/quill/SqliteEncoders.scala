package scommons.websql.quill

import java.time.LocalDate
import java.util.{Date, UUID}

import scala.scalajs.js
import scala.scalajs.js.typedarray._

/**
  * @see https://www.sqlite.org/datatype3.html
  */
trait SqliteEncoders {
  this: SqliteContext[_] =>

  type Encoder[T] = SqliteEncoder[T]

  case class SqliteEncoder[T](encoder: BaseEncoder[T]) extends BaseEncoder[T] {
    override def apply(index: Index, value: T, row: PrepareRow) =
      encoder(index, value, row)
  }

  private def encoderUnsafe[T]: Encoder[T] = SqliteEncoder { (_: Index, value: T, row: PrepareRow) =>
    row :+ value.asInstanceOf[js.Any]
  }

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    SqliteEncoder((index: Index, value: I, row: PrepareRow) => e(index, mapped.f(value), row))

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] =
    SqliteEncoder { (index: Index, value: Option[T], row: PrepareRow) =>
      value match {
        case None    => row :+ (null: js.Any)
        case Some(v) => row :+ e(index, v, Nil).head
      }
    }

  implicit val booleanToDouble: MappedEncoding[Boolean, Double] = MappedEncoding(if (_) 1 else 0)
  implicit val bigDecimalToDouble: MappedEncoding[BigDecimal, Double] = MappedEncoding(_.toDouble)
  implicit val byteToDouble: MappedEncoding[Byte, Double] = MappedEncoding(_.toDouble)
  implicit val shortToDouble: MappedEncoding[Short, Double] = MappedEncoding(_.toDouble)
  implicit val intToDouble: MappedEncoding[Int, Double] = MappedEncoding(_.toDouble)
  implicit val longToDouble: MappedEncoding[Long, Double] = MappedEncoding(_.toDouble)
  implicit val floatToDouble: MappedEncoding[Float, Double] = MappedEncoding(_.toDouble)
  implicit val uuidToString: MappedEncoding[UUID, String] = MappedEncoding(_.toString)
  implicit val byteArrayToInt8Array: MappedEncoding[Array[Byte], Int8Array] = MappedEncoding(_.toTypedArray)
  implicit val byteSeqToInt8Array: MappedEncoding[Seq[Byte], Int8Array] = MappedEncoding(_.toArray.toTypedArray)
  
  implicit val stringEncoder: Encoder[String] = encoderUnsafe[String]
  implicit val doubleEncoder: Encoder[Double] = encoderUnsafe[Double]
  implicit val int8ArrayEncoder: Encoder[Int8Array] = encoderUnsafe[Int8Array]
  
  implicit val booleanEncoder: Encoder[Boolean] = mappedEncoder[Boolean, Double]
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = mappedEncoder[BigDecimal, Double]
  implicit val byteEncoder: Encoder[Byte] = mappedEncoder[Byte, Double]
  implicit val shortEncoder: Encoder[Short] = mappedEncoder[Short, Double]
  implicit val intEncoder: Encoder[Int] = mappedEncoder[Int, Double]
  implicit val longEncoder: Encoder[Long] = mappedEncoder[Long, Double]
  implicit val floatEncoder: Encoder[Float] = mappedEncoder[Float, Double]
  implicit val uuidEncoder: Encoder[UUID] = mappedEncoder[UUID, String]
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = mappedEncoder[Array[Byte], Int8Array]
  implicit val byteSeqEncoder: Encoder[Seq[Byte]] = mappedEncoder[Seq[Byte], Int8Array]
  
  implicit val dateEncoder: Encoder[Date] =
    SqliteEncoder { (_: Index, value: Date, row: PrepareRow) =>
      row :+ (value.getTime.toDouble: js.Any)
    }

  implicit val localDateEncoder: Encoder[LocalDate] =
    SqliteEncoder { (_: Index, value: LocalDate, row: PrepareRow) =>
      val millis: js.Any = {
        new js.Date(value.getYear, value.getMonthValue - 1, value.getDayOfMonth)
          .getTime()
      }
      row :+ millis
    }
}
