package showcase.domain.dao

import scommons.websql.quill.dao.CommonDao
import showcase.domain._

import scala.concurrent.Future

class ProductDao(val ctx: ShowcaseDBContext) extends CommonDao
  with ProductSchema
  with CategorySchema {

  import ctx._

  def allProducts(): Future[Seq[ProductEntity]] = {
    val q = ctx.run(
      products.sortBy(_.id)
    )

    ctx.performIO(q)
  }
  
  def joinProducts(): Future[Seq[(ProductEntity, CategoryEntity)]] = {
    val q = ctx.run {
      for {
        p <- products.sortBy(_.id)
        c <- categories.join(c => p.categoryId.contains(c.id))
      } yield (p, c)
    }

    ctx.performIO(q)
  }
  
  def leftJoinProducts(): Future[Seq[(ProductEntity, Option[CategoryEntity])]] = {
    val q = ctx.run(
      products.sortBy(_.id)
        .leftJoin(categories).on((p, c) => p.categoryId.contains(c.id))
    )

    ctx.performIO(q)
  }
}
