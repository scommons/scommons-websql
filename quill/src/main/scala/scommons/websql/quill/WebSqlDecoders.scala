package scommons.websql.quill

import java.time.LocalDate
import java.util.{Date, UUID}

import io.getquill.context.Context
import io.getquill.context.mirror.Row

import scala.reflect.ClassTag

trait WebSqlDecoders {
  this: Context[_, _] =>

  type ResultRow = Row
  type Decoder[T] = WebSqlDecoder[T]

  case class WebSqlDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow) =
      decoder(index, row)
  }

  def decoder[T: ClassTag]: Decoder[T] = WebSqlDecoder((index: Index, row: ResultRow) => row[T](index))

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    WebSqlDecoder((index: Index, row: ResultRow) => mapped.f(d.apply(index, row)))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    WebSqlDecoder((index: Index, row: ResultRow) =>
      row[Option[Any]](index) match {
        case Some(v) => Some(d(0, Row(v)))
        case None    => None
      })

  implicit val doubleToBigDecimal: MappedEncoding[Double, BigDecimal] = MappedEncoding(BigDecimal(_))
  implicit val doubleToByte: MappedEncoding[Double, Byte] = MappedEncoding(_.toByte)
  implicit val doubleToShort: MappedEncoding[Double, Short] = MappedEncoding(_.toShort)
  implicit val doubleToInt: MappedEncoding[Double, Int] = MappedEncoding(_.toInt)
  implicit val doubleToLong: MappedEncoding[Double, Long] = MappedEncoding(_.toLong)
  implicit val doubleToFloat: MappedEncoding[Double, Float] = MappedEncoding(_.toFloat)
  
  implicit val stringDecoder: Decoder[String] = decoder[String]
  implicit val doubleDecoder: Decoder[Double] = decoder[Double]
  implicit val booleanDecoder: Decoder[Boolean] = decoder[Boolean]
  
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = mappedDecoder[Double, BigDecimal]
  implicit val byteDecoder: Decoder[Byte] = mappedDecoder[Double, Byte]
  implicit val shortDecoder: Decoder[Short] = mappedDecoder[Double, Short]
  implicit val intDecoder: Decoder[Int] = mappedDecoder[Double, Int]
  implicit val longDecoder: Decoder[Long] = mappedDecoder[Double, Long]
  implicit val floatDecoder: Decoder[Float] = mappedDecoder[Double, Float]
  
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder[Array[Byte]]
  implicit val dateDecoder: Decoder[Date] = decoder[Date]
  implicit val localDateDecoder: Decoder[LocalDate] = decoder[LocalDate]
  implicit val uuidDecoder: Decoder[UUID] = decoder[UUID]
}
