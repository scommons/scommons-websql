package scommons.websql.io.showcase

import scommons.websql.io.showcase.domain.CategoryEntity
import scommons.websql.io.showcase.domain.dao.CategoryDao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CategoryService(dao: CategoryDao) {

  def getById(id: Int): Future[CategoryEntity] = {
    dao.getById(id).map(ensureCategory(id, _))
  }
  
  def add(entity: CategoryEntity): Future[CategoryEntity] = {
    for {
      insertId <- dao.insert(entity)
      entity <- dao.getById(insertId).map(ensureCategory(insertId, _))
    } yield entity
  }
  
  private def ensureCategory(id: Int, maybeCat: Option[CategoryEntity]): CategoryEntity = {
    maybeCat.getOrElse {
      throw new IllegalArgumentException(s"Category is not found, categoryId: $id")
    }
  }
}
