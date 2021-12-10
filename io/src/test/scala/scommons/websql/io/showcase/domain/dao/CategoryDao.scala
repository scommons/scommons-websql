package scommons.websql.io.showcase.domain.dao

import scommons.websql.io.dao.CommonDao
import scommons.websql.io.showcase.domain._

import scala.concurrent.Future

class CategoryDao(val ctx: ShowcaseDBContext) extends CommonDao {
  
  import ctx._

  def getByIdQuery(id: Int): IO[Seq[CategoryEntity], Effect.Read] = {
    ctx.runQuery(
      sql = "SELECT id, category_name FROM categories WHERE id = ?",
      args = id,
      extractor = CategoryEntity.tupled
    )
  }

  def getById(id: Int): Future[Option[CategoryEntity]] = {
    getOne("getById", ctx.performIO(getByIdQuery(id)))
  }

  def count(): Future[Int] = {
    ctx.performIO(
      ctx.runQuerySingle("SELECT count(*) FROM categories", identity[Int])
    )
  }

  def list(optOffset: Option[Int],
           limit: Int,
           symbols: Option[String]
          ): Future[(Seq[CategoryEntity], Option[Int])] = {

    val text = s"%${symbols.getOrElse("")}%"
    val offset = optOffset.getOrElse(0)

    val countQuery = optOffset match {
      case Some(_) => IO.successful(None)
      case None => ctx.runQuerySingle(
        sql = "SELECT count(*) FROM categories WHERE category_name LIKE ?",
        args = text,
        extractor = identity[Int]
      ).map(Some(_))
    }

    val fetchQuery = ctx.runQuery(
      sql =
        """SELECT
          |  id,
          |  category_name
          |FROM
          |  categories
          |WHERE
          |  category_name LIKE ?
          |ORDER BY
          |  category_name
          |LIMIT ?
          |OFFSET ?
          |""".stripMargin,
      args = (text, limit, offset),
      extractor = CategoryEntity.tupled
    )

    val q = for {
      maybeCount <- countQuery
      results <- fetchQuery
    } yield {
      (results, maybeCount)
    }

    // internally IO is always performed within transaction
    ctx.performIO(q)
  }

  def insertQuery(entity: CategoryEntity): IO[Int, Effect.Write] = {
    ctx.runActionReturning(
      "INSERT INTO categories (category_name) VALUES (?)", entity.categoryName
    ).map(_.toInt)
  }

  def insert(entity: CategoryEntity): Future[Int] = {
    ctx.performIO(insertQuery(entity))
  }

  def insertMany(list: Seq[CategoryEntity]): Future[Seq[Int]] = {
    ctx.performIO(IO.sequence(list.map(insertQuery)))
  }

  def upsert(entity: CategoryEntity): Future[CategoryEntity] = {
    val q = for {
      maybeCategory <- ctx.runQuery(
        sql = "SELECT id, category_name FROM categories WHERE category_name = ?",
        args = entity.categoryName,
        extractor = CategoryEntity.tupled
      ).map(_.headOption)
      id <- maybeCategory match {
        case None => insertQuery(entity)
        case Some(c) =>
          updateQuery(entity.copy(id = c.id))
            .map(_ => c.id)
      }
      res <- getByIdQuery(id).map(_.head)
    } yield res

    ctx.performIO(q)
  }

  def updateQuery(entity: CategoryEntity): IO[Long, Effect.Write] = {
    ctx.runAction(
      sql = "UPDATE categories SET category_name = ? WHERE id = ?",
      args = (entity.categoryName, entity.id)
    )
  }

  def update(entity: CategoryEntity): Future[Boolean] = {
    isUpdated(ctx.performIO(updateQuery(entity)))
  }

  def updateMany(list: Seq[CategoryEntity]): Future[Seq[Boolean]] = {
    ctx.performIO(IO.sequence(list.map(updateQuery)).map { results =>
      results.map(_ > 0)
    })
  }

  def deleteAll(): Future[Long] = {
    ctx.performIO(ctx.runAction("DELETE FROM categories"))
  }
}
