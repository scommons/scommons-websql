package scommons.websql.quill.dao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class CommonDao {

  def getOne[T](queryName: String, resultsF: Future[Seq[T]]): Future[Option[T]] = {
    resultsF.map { results =>
      val size = results.size
      if (size > 1) {
        val query = s"${getClass.getSimpleName}.$queryName"
        throw new IllegalStateException(s"Expected only single result, but got $size in $query")
      }

      results.headOption
    }
  }

  def isUpdated(resultF: Future[Long]): Future[Boolean] = {
    resultF.map(_ > 0)
  }
}
