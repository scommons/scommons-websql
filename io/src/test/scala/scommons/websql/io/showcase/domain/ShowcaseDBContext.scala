package scommons.websql.io.showcase.domain

import scommons.websql.Database
import scommons.websql.io.SqliteContext

class ShowcaseDBContext(db: Database) extends SqliteContext(db) {

  // example of custom encoder
  implicit val categoryIdToInt: MappedEncoding[CategoryId, Int] = mappedEncoding[CategoryId, Int](_.value)
  implicit val categoryIdEncoder: Encoder[CategoryId] = mappedEncoder[CategoryId, Int]

  // example of custom decoder
  implicit val intToCategoryId: MappedEncoding[Int, CategoryId] = mappedEncoding[Int, CategoryId](CategoryId)
  implicit val categoryIdDecoder: Decoder[CategoryId] = mappedDecoder[Int, CategoryId]
}
