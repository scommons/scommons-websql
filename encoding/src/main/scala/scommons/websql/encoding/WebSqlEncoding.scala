package scommons.websql.encoding

trait WebSqlEncoding extends BaseEncodingDsl {

  type MappedEncoding[I, O] = scommons.websql.encoding.MappedEncoding[I, O]

  def mappedEncoding[I, O](f: I => O): MappedEncoding[I, O] = scommons.websql.encoding.MappedEncoding(f)

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    WebSqlEncoder((index: Index, value: I, row: PrepareRow) => e(index, mapped.f(value), row))

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    WebSqlDecoder((index: Index, row: ResultRow) => mapped.f(d.apply(index, row)))

  implicit def seqEncoder[T](implicit e: Encoder[T]): Encoder[Seq[T]] =
    WebSqlEncoder { (_: Index, seq: Seq[T], row: PrepareRow) =>
      val values = seq.map(v => e(-1, v, Nil).head)
      row :++ values
    }
}
