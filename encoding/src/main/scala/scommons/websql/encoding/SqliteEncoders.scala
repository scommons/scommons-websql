package scommons.websql.encoding

import java.util.{Date, UUID}
import scala.scalajs.js
import scala.scalajs.js.typedarray._

/**
 * @see https://www.sqlite.org/datatype3.html
 */
trait SqliteEncoders extends BaseEncodingDsl {

  private def encoderUnsafe[T]: Encoder[T] = WebSqlEncoder { (_: Index, value: T, row: PrepareRow) =>
    row :+ value.asInstanceOf[js.Any]
  }

  implicit def optionEncoder[T](implicit e: Encoder[T]): Encoder[Option[T]] =
    WebSqlEncoder { (_: Index, value: Option[T], row: PrepareRow) =>
      value match {
        case None => row :+ (null: js.Any)
        case Some(v) => e(-1, v, row)
      }
    }

  implicit val booleanToDouble: MappedEncoding[Boolean, Double] = mappedEncoding[Boolean, Double](if (_) 1 else 0)
  implicit val bigDecimalToDouble: MappedEncoding[BigDecimal, Double] = mappedEncoding[BigDecimal, Double](_.toDouble)
  implicit val byteToDouble: MappedEncoding[Byte, Double] = mappedEncoding[Byte, Double](_.toDouble)
  implicit val shortToDouble: MappedEncoding[Short, Double] = mappedEncoding[Short, Double](_.toDouble)
  implicit val intToDouble: MappedEncoding[Int, Double] = mappedEncoding[Int, Double](_.toDouble)
  implicit val longToDouble: MappedEncoding[Long, Double] = mappedEncoding[Long, Double](_.toDouble)
  implicit val floatToDouble: MappedEncoding[Float, Double] = mappedEncoding[Float, Double](_.toDouble)
  implicit val uuidToString: MappedEncoding[UUID, String] = mappedEncoding[UUID, String](_.toString)

  implicit val byteArrayToInt8Array: MappedEncoding[Array[Byte], Int8Array] =
    mappedEncoding[Array[Byte], Int8Array](_.toTypedArray)

  implicit val byteSeqToInt8Array: MappedEncoding[Seq[Byte], Int8Array] =
    mappedEncoding[Seq[Byte], Int8Array](_.toArray.toTypedArray)

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
    WebSqlEncoder { (_: Index, value: Date, row: PrepareRow) =>
      row :+ (value.getTime.toDouble: js.Any)
    }
}
