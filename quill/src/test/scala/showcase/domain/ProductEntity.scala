package showcase.domain

case class ProductEntity(id: Int,
                         name: String,
                         categoryId: Option[Int])

//noinspection TypeAnnotation
trait ProductSchema {

  val ctx: ShowcaseDBContext
  import ctx._

  val products = quote(querySchema[ProductEntity]("products"))
}
