// Copied from io.getquill.dsl.EncodingDsl
package scommons.websql.encoding

import scommons.websql.WebSqlRow

import scala.scalajs.js

trait BaseEncodingDsl {

  type PrepareRow = List[js.Any]
  type ResultRow = WebSqlRow
  type Index = Int

  type BaseEncoder[T] = (Index, T, PrepareRow) => PrepareRow
  type Encoder[T] = WebSqlEncoder[T]
  type BaseDecoder[T] = (Index, ResultRow) => T
  type Decoder[T] = WebSqlDecoder[T]

  type MappedEncoding[I, O]

  def mappedEncoding[I, O](f: I => O): MappedEncoding[I, O]

  case class WebSqlEncoder[T](encoder: BaseEncoder[T]) extends BaseEncoder[T] {
    def apply(index: Index, value: T, row: PrepareRow): PrepareRow =
      encoder(index, value, row)
  }

  case class WebSqlDecoder[T](decoder: BaseDecoder[T]) extends BaseDecoder[T] {
    def apply(index: Index, row: ResultRow): T = decoder(index, row)
  }

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I]

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O]

  implicit def stringEncoder: Encoder[String]

  implicit def bigDecimalEncoder: Encoder[BigDecimal]

  implicit def booleanEncoder: Encoder[Boolean]

  implicit def byteEncoder: Encoder[Byte]

  implicit def shortEncoder: Encoder[Short]

  implicit def intEncoder: Encoder[Int]

  implicit def longEncoder: Encoder[Long]

  implicit def doubleEncoder: Encoder[Double]
}
