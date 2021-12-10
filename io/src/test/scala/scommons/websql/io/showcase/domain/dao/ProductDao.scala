package scommons.websql.io.showcase.domain.dao

import scommons.websql.io.dao.CommonDao
import scommons.websql.io.showcase.domain._

import scala.concurrent.Future

class ProductDao(val ctx: ShowcaseDBContext) extends CommonDao {
  
  import ctx._

  def allProducts(): Future[Seq[ProductEntity]] = {
    ctx.performIO(ctx.runQuery(
      "SELECT id, name, category_id FROM products ORDER BY id",
      ProductEntity.tupled
    ))
  }
  
  def joinProducts(): Future[Seq[(ProductEntity, CategoryEntity)]] = {
    ctx.performIO(ctx.runQuery(
      sql =
        """SELECT
          |  p.id AS _0, p.name AS _1, p.category_id AS _2, c.id AS _4, c.category_name AS _3
          |FROM (
          |  SELECT id, name, category_id FROM products ORDER BY id
          |) AS p
          |INNER JOIN categories c ON p.category_id = c.id
          |""".stripMargin,
      extractor = { case (pId, pName, pcId, cId, cName) =>
        (ProductEntity(pId, pName, pcId), CategoryEntity(cId, cName))
      }: ((Int, String, Option[Int], Int, String)) =>
        (ProductEntity, CategoryEntity)
    ))
  }
  
  def leftJoinProducts(): Future[Seq[(ProductEntity, Option[CategoryEntity])]] = {
    ctx.performIO(ctx.runQuery(
      sql =
        """SELECT
          |  p.id AS _0, p.name AS _1, p.category_id AS _2, c.id AS _3, c.category_name AS _4
          |FROM (
          |  SELECT id, name, category_id FROM products ORDER BY id
          |) AS p
          |LEFT JOIN categories c ON p.category_id = c.id
          |""".stripMargin,
      extractor = { case (pId, pName, pcId, cId, cName) =>
        (ProductEntity(pId, pName, pcId), cId.flatMap { id =>
          cName.map(name => CategoryEntity(id, name))
        })
      }: ((Int, String, Option[Int], Option[Int], Option[String])) =>
        (ProductEntity, Option[CategoryEntity])
    ))
  }
}
