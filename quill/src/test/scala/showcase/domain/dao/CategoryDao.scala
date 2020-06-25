package showcase.domain.dao

import scommons.websql.quill.dao.CommonDao
import showcase.domain._

import scala.concurrent.Future

class CategoryDao(val ctx: ShowcaseDBContext) extends CommonDao
  with CategorySchema {

  import ctx._

  def getByIdQuery(id: Int): IO[Seq[CategoryEntity], Effect.Read] = {
    ctx.run(categories
      .filter(c => c.id == lift(id))
    )
  }

  def getById(id: Int): Future[Option[CategoryEntity]] = {
    getOne("getById", ctx.performIO(getByIdQuery(id)))
  }

  def count(): Future[Int] = {
    ctx.performIO(ctx.run(categories
      .size
    ).map(_.toInt))
  }

  def list(optOffset: Option[Int],
           limit: Int,
           symbols: Option[String]
          ): Future[(Seq[CategoryEntity], Option[Int])] = {

    val textLower = s"%${symbols.getOrElse("").trim.toLowerCase}%"
    val offset = optOffset.getOrElse(0)

    val countQuery = optOffset match {
      case Some(_) => IO.successful(None)
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

    val q = for {
      maybeCount <- countQuery
      results <- fetchQuery
    } yield {
      (results, maybeCount.map(_.toInt))
    }

    // internally IO is always performed within transaction
    // so, explicitly specifying transactional has no additional effect
    //
    ctx.performIO(q.transactional)
  }

  def insertQuery(entity: CategoryEntity): IO[Int, Effect.Write] = {
    ctx.run(categories
      .insert(lift(entity))
      .returning(_.id)
    ).map(_.toInt)
  }
  
  def insert(entity: CategoryEntity): Future[Int] = {
    ctx.performIO(insertQuery(entity))
  }

  def insertMany(list: Seq[CategoryEntity]): Future[Seq[Int]] = {
    val q = quote {
      liftQuery(list).foreach { entity =>
        categories
          .insert(entity)
          .returning(_.id)
      }
    }

    ctx.performIO(ctx.run(q).map(_.map(_.toInt)))
  }

  def upsert(entity: CategoryEntity): Future[CategoryEntity] = {
    val q = for {
      maybeCategory <- ctx.run(categories
        .filter(c => c.categoryName == lift(entity.categoryName))
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
    ctx.run(categories
      .filter(c => c.id == lift(entity.id))
      .update(lift(entity))
    )
  }
  
  def update(entity: CategoryEntity): Future[Boolean] = {
    isUpdated(ctx.performIO(updateQuery(entity)))
  }

  def updateMany(list: Seq[CategoryEntity]): Future[Seq[Boolean]] = {
    val q = quote {
      liftQuery(list).foreach { entity =>
        categories
          .filter(_.id == entity.id)
          .update(entity)
      }
    }

    ctx.performIO(ctx.run(q).map { results =>
      results.map(_ > 0)
    })
  }

  def deleteAll(): Future[Long] = {
    ctx.performIO(ctx.run(categories.delete))
  }
}
