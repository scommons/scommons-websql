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
  
  it should "return count of records" in {
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
  
  it should "return empty results" in {
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
  
  it should "return non-empty results" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    //when
    val result = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.list(Some(1), 10, Some("Test Category"))
    }

    //then
    result.map { res =>
      res shouldBe {
        (Seq(
          CategoryEntity(2, "test category 2")
        ), None)
      }
    }
  }
  
  it should "combine queries using Future.sequence" in {
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
  
  it should "combine queries using for-comprehension" in {
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
  
  it should "fail if queries are inside for-comprehension" in {
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
          "Transaction is already finalized." +
            " Use Future.sequence or run queries outside for-comprehension."
        }
    }
  }
  
  it should "update record" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    val beforeF = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.list(None, 10, None)
    }.map { before =>
      before shouldBe {
        (Seq(
          CategoryEntity(1, "test category 1"),
          CategoryEntity(2, "test category 2")
        ), Some(2))
      }
      before
    }

    beforeF.flatMap { before =>
      //when
      ctx.transaction { implicit tx =>
        val updateF = dao.update(before._1.head.copy(categoryName = "updated category"))
        val fetchF = dao.list(None, 10, None)
        for {
          isUpdated <- updateF
          results <- fetchF
        } yield {
          (isUpdated, results)
        }
      }.map { case (isUpdated, results) =>
        //then
        isUpdated shouldBe true
        results shouldBe {
          (Seq(
            CategoryEntity(2, "test category 2"),
            CategoryEntity(1, "updated category")
          ), Some(2))
        }
      }
    }
  }
  
  it should "insert record" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    val beforeF = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.list(None, 10, None)
    }.map { before =>
      before shouldBe {
        (Seq(
          CategoryEntity(1, "test category 1"),
          CategoryEntity(2, "test category 2")
        ), Some(2))
      }
      before
    }

    beforeF.flatMap { _ =>
      //when
      ctx.transaction { implicit tx =>
        val insertF = dao.insert(CategoryEntity(-1, "new category"))
        val fetchF = dao.list(None, 10, None)
        for {
          insertId <- insertF
          results <- fetchF
        } yield {
          (insertId, results)
        }
      }.map { case (insertId, results) =>
        //then
        insertId shouldBe 3
        results shouldBe {
          (Seq(
            CategoryEntity(3, "new category"),
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(3))
        }
      }
    }
  }
  
  it should "delete records" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    val beforeF = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.list(None, 10, None)
    }.map { before =>
      before shouldBe {
        (Seq(
          CategoryEntity(1, "test category 1"),
          CategoryEntity(2, "test category 2")
        ), Some(2))
      }
      before
    }

    beforeF.flatMap { _ =>
      //when
      ctx.transaction { implicit tx =>
        val deleteF = dao.deleteAll()
        val fetchF = dao.list(None, 10, None)
        for {
          deleted <- deleteF
          results <- fetchF
        } yield {
          (deleted, results)
        }
      }.map { case (deleted, results) =>
        //then
        deleted shouldBe 2
        results shouldBe {
          (Nil, Some(0))
        }
      }
    }
  }

  it should "do batch update" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    val beforeF = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.list(None, 10, None)
    }.map { before =>
      before shouldBe {
        (Seq(
          CategoryEntity(1, "test category 1"),
          CategoryEntity(2, "test category 2")
        ), Some(2))
      }
      before
    }

    beforeF.flatMap { before =>
      //when
      ctx.transaction { implicit tx =>
        val updateF = dao.updateMany(before._1.zipWithIndex.map { case (c, i) =>
          c.copy(categoryName = s"updated category $i")
        })
        val fetchF = dao.list(None, 10, None)
        for {
          updated <- updateF
          results <- fetchF
        } yield {
          (updated, results)
        }
      }.map { case (updated, results) =>
        //then
        updated shouldBe Seq(true, true)
        results shouldBe {
          (Seq(
            CategoryEntity(1, "updated category 0"),
            CategoryEntity(2, "updated category 1")
          ), Some(2))
        }
      }
    }
  }

  it should "do batch insert" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    val beforeF = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.list(None, 10, None)
    }.map { before =>
      before shouldBe {
        (Seq(
          CategoryEntity(1, "test category 1"),
          CategoryEntity(2, "test category 2")
        ), Some(2))
      }
      before
    }

    beforeF.flatMap { _ =>
      //when
      ctx.transaction { implicit tx =>
        val insertF = dao.insertMany(Seq(
          CategoryEntity(-1, "new category 1"),
          CategoryEntity(-1, "new category 2")
        ))
        val fetchF = dao.list(None, 10, None)
        for {
          insertIds <- insertF
          results <- fetchF
        } yield {
          (insertIds, results)
        }
      }.map { case (insertIds, results) =>
        //then
        insertIds shouldBe Seq(3, 4)
        results shouldBe {
          (Seq(
            CategoryEntity(3, "new category 1"),
            CategoryEntity(4, "new category 2"),
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(4))
        }
      }
    }
  }

  it should "rollback transaction" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val ctx = new TestSqliteContext(db)
    val dao = new CategoryDao(ctx)

    val beforeF = ctx.transaction { implicit tx =>
      prepareDb(tx)
      dao.list(None, 10, None)
    }.map { before =>
      before shouldBe {
        (Seq(
          CategoryEntity(1, "test category 1"),
          CategoryEntity(2, "test category 2")
        ), Some(2))
      }
      before
    }

    beforeF.flatMap { before =>
      //when
      ctx.transaction { implicit tx =>
        Future.sequence(Seq(
          dao.insert(CategoryEntity(-1, "new category")),
          dao.insert(before._1.head)
        ))
      }.failed.flatMap { error =>
        //then
        error.getMessage shouldBe {
          "Error: SQLITE_CONSTRAINT: UNIQUE constraint failed: categories.category_name"
        }
        ctx.transaction { implicit tx =>
          dao.list(None, 10, None)
        }.map { after =>
          after shouldBe before
        }
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
    extends CommonDao
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

  abstract class CommonDao(implicit ec: ExecutionContext) {

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

    def isUpdated[T](resultF: Future[Int]): Future[Boolean] = {
      resultF.map(_ > 0)
    }
  }
}
