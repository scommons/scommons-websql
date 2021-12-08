package scommons.websql.quill

import io.getquill.dsl.EncodingDsl
import scommons.websql.WebSqlRow
import scommons.websql.encoding.BaseEncodingDsl

import scala.scalajs.js

trait WebSqlQuillEncoding extends BaseEncodingDsl with EncodingDsl {
  this: WebSqlContext[_, _] =>

  override type PrepareRow = List[js.Any]
  override type ResultRow = WebSqlRow
  override type Index = Int
  
  override type BaseEncoder[T] = (Index, T, PrepareRow) => PrepareRow
  override type BaseDecoder[T] = (Index, ResultRow) => T

  override type MappedEncoding[I, O] = io.getquill.MappedEncoding[I, O]
  def mappedEncoding[I, O](f: I => O): MappedEncoding[I, O] = io.getquill.MappedEncoding(f)

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    WebSqlEncoder((index: Index, value: I, row: PrepareRow) => e(index, mapped.f(value), row))

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    WebSqlDecoder((index: Index, row: ResultRow) => mapped.f(d.apply(index, row)))
}
