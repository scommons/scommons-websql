package showcase.domain.dao

import scommons.websql.Transaction
import scommons.websql.quill.dao.CommonDao
import showcase.domain._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CategoryDao(val ctx: ShowcaseDBContext) extends CommonDao
  with CategorySchema {

  import ctx._

  def getById(id: Int)(implicit tx: Transaction): Future[Option[CategoryEntity]] = {
    getOne("getById", ctx.run(categories
      .filter(c => c.id == lift(id))
    ))
  }

  def count()(implicit tx: Transaction): Future[Int] = {
    ctx.run(categories
      .size
    ).map(_.toInt)
  }

  def list(optOffset: Option[Int],
           limit: Int,
           symbols: Option[String]
          )(implicit tx: Transaction): Future[(Seq[CategoryEntity], Option[Int])] = {

    val textLower = s"%${symbols.getOrElse("").trim.toLowerCase}%"
    val offset = optOffset.getOrElse(0)

    val countQuery = optOffset match {
      case Some(_) => Future.successful(None)
      case None => ctx.run(categories
        .filter(c => c.categoryName.toLowerCase.like(lift(textLower)))
        .size
      ).map(Some(_))
    }
    
    val fetchQuery = ctx.run(categories
      .filter(_.categoryName.toLowerCase.like(lift(textLower)))
      .sortBy(_.categoryName)
      .drop(lift(offset))
      .take(lift(limit))
    )

    // Important:
    //   queries within transaction should be run outside for-comprehension
    //
    for {
      maybeCount <- countQuery
      results <- fetchQuery
    } yield {
      (results, maybeCount.map(_.toInt))
    }
  }

  def insert(entity: CategoryEntity)(implicit tx: Transaction): Future[Int] = {
    ctx.run(categories
      .insert(lift(entity))
      .returning(_.id)
    )
  }

  def insertMany(list: Seq[CategoryEntity])(implicit tx: Transaction): Future[Seq[Int]] = {
    val q = quote {
      liftQuery(list).foreach { entity =>
        categories
          .insert(entity)
          .returning(_.id)
      }
    }

    ctx.run(q)
  }

  def update(entity: CategoryEntity)(implicit tx: Transaction): Future[Boolean] = {
    isUpdated(ctx.run(categories
      .filter(c => c.id == lift(entity.id))
      .update(lift(entity))
    ))
  }

  def updateMany(list: Seq[CategoryEntity])(implicit tx: Transaction): Future[Seq[Boolean]] = {
    val q = quote {
      liftQuery(list).foreach { entity =>
        categories
          .filter(_.id == entity.id)
          .update(entity)
      }
    }

    ctx.run(q).map { results =>
      results.map(_ > 0)
    }
  }

  def deleteAll()(implicit tx: Transaction): Future[Int] = {
    ctx.run(categories.delete)
  }
}
