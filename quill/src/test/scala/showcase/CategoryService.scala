package showcase

import showcase.domain.CategoryEntity
import showcase.domain.dao.CategoryDao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CategoryService(dao: CategoryDao) {

  import dao.ctx

  def getById(id: Int): Future[CategoryEntity] = {
    ctx.transaction { implicit tx =>
      dao.getById(id).map { maybeCat =>
        maybeCat.getOrElse {
          throw new IllegalArgumentException(s"Category is not found, categoryId: $id")
        }
      }
    }
  }
  
  def add(entity: CategoryEntity): Future[CategoryEntity] = {
    for {
      insertId <- ctx.transaction { implicit tx =>
        dao.insert(entity)
      } 
      entity <- getById(insertId)
    } yield entity
  }
}
