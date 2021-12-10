package scommons.websql.io.showcase.domain

case class ProductEntity(id: Int,
                         name: String,
                         categoryId: Option[Int])
