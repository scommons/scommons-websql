package scommons.websql.quill

import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.{Transaction, WebSQL}
import showcase.domain.dao.CategoryDao
import showcase.domain.{CategoryEntity, ShowcaseDBContext}

import scala.concurrent.Future
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
    val ctx = new ShowcaseDBContext(WebSQL.openDatabase(":memory:"))
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
