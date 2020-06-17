package scommons.websql.quill

import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.quill.SqliteContextSpec._
import scommons.websql.{Transaction, WebSQL}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SqliteContextSpec extends AsyncTestSpec {

  private def prepareDb(tx: Transaction): Unit = {
    tx.executeSql(
      """create table categories (
        |  id              integer primary key,
        |  category_name   text not null,
        |  created_at      timestamp without time zone default current_timestamp,
        |  UNIQUE (category_name)
        |)
        |""".stripMargin
    )
    tx.executeSql(
      "insert into categories (category_name) values (?), (?)",
      Seq(
        "test category 1",
        "test category 2"
      )
    )
  }
  
  "executeQuery" should "return count of records from DB" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    //when
    val result = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.count()
    }

    //then
    result.map { res =>
      res shouldBe 2
    }
  }
  
  it should "return empty results from DB" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    //when
    val result = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.getById(123)
    }

    //then
    result.map { res =>
      res shouldBe None
    }
  }
  
  it should "return non-empty results from DB" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    //when
    val result = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.getById(2)
    }

    //then
    result.map { res =>
      res shouldBe Some(CategoryEntity(2, "test category 2"))
    }
  }
  
  it should "combine several calls using Future.sequence" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    //when
    val result = ctx.transaction { implicit tx =>
      prepareDb(tx)
      Future.sequence(Seq(
        dao.getById(1),
        dao.getById(2)
      ))
    }

    //then
    result.map { res =>
      res shouldBe Seq(
        Some(CategoryEntity(1, "test category 1")),
        Some(CategoryEntity(2, "test category 2"))
      )
    }
  }
  
  it should "combine several calls using for-comprehension" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    //when
    val result = ctx.transaction { implicit tx =>
      prepareDb(tx)
      val res1Future = dao.getById(1)
      val res2Future = dao.getById(2)
      for {
        res1 <- res1Future
        res2 <- res2Future
      } yield {
        (res1, res2)
      }
    }

    //then
    result.map { res =>
      res shouldBe {
        (Some(CategoryEntity(1, "test category 1")),
          Some(CategoryEntity(2, "test category 2")))
      }
    }
  }
  
  it should "fail to combine several calls inside for-comprehension" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    //when
    val result = ctx.transaction { implicit tx =>
      prepareDb(tx)
      for {
        res1 <- dao.getById(1)
        res2 <- dao.getById(2)
      } yield {
        (res1, res2)
      }
    }

    //then
    result.failed.map {
      case NonFatal(ex) =>
        ex.getMessage shouldBe {
          "Transaction is already finalized. Use Future.sequence or run queries outside for-comprehension."
        }
    }
  }
}

object SqliteContextSpec {

  case class CategoryEntity(id: Int, categoryName: String)

  //noinspection TypeAnnotation
  trait CategorySchema {

    val ctx: TestSqliteContext
    import ctx._

    implicit val categoriesInsertMeta = insertMeta[CategoryEntity](
      _.id
    )
    implicit val categoriesUpdateMeta = updateMeta[CategoryEntity](
      _.id
    )

    val categories = quote(querySchema[CategoryEntity]("categories"))
  }

  class CategoryDao(val ctx: TestSqliteContext)(implicit ex: ExecutionContext)
    extends CategorySchema {

    import ctx._

    def getById(id: Int)(implicit tx: Transaction): Future[Option[CategoryEntity]] = {
      ctx.run(categories
        .filter(c => c.id == lift(id))
      ).map(_.headOption)
    }

    def count()(implicit tx: Transaction): Future[Int] = {
      ctx.run(categories
        .size
      ).map(_.toInt)
    }

    def deleteAll() = {
      ctx.run(categories.delete)
    }
  }
}
