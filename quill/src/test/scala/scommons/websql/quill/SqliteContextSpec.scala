package scommons.websql.quill

import org.scalatest.Succeeded
import scommons.nodejs.test.AsyncTestSpec
import scommons.websql.{Database, Transaction, WebSQL}
import showcase.domain.dao.CategoryDao
import showcase.domain.{CategoryEntity, ShowcaseDBContext}

import scala.scalajs.js
import scala.util.Success

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
  
  it should "do nothing when close()" in {
    //given
    val ctx = new ShowcaseDBContext(mock[Database])
    
    //when
    ctx.close()
    
    //then
    Succeeded
  }
  
  it should "do nothing when probe()" in {
    //given
    val ctx = new ShowcaseDBContext(mock[Database])
    
    //when
    val result = ctx.probe("test")
    
    //then
    result shouldBe Success(())
  }
  
  it should "return count of records" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    //when
    val result = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
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
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    //when
    val result = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
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
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    //when
    val result = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
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
  
  it should "combine queries using IO.sequence" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    //when
    val result = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.ctx.performIO(dao.ctx.IO.sequence(Seq(
        dao.getByIdQuery(1).map(_.headOption),
        dao.getByIdQuery(2).map(_.headOption)
      )))
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
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    //when
    val result = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      val io = for {
        res1 <- dao.getByIdQuery(1).map(_.headOption)
        res2 <- dao.getByIdQuery(2).map(_.headOption)
      } yield {
        (res1, res2)
      }
      dao.ctx.performIO(io)
    }

    //then
    result.map { res =>
      res shouldBe {
        (Some(CategoryEntity(1, "test category 1")),
          Some(CategoryEntity(2, "test category 2")))
      }
    }
  }
  
  it should "update record" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    val beforeF = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.list(None, 10, None).map { before =>
        before shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
        before
      }
    }

    beforeF.flatMap { before =>
      //when
      {
        for {
          isUpdated <- dao.update(before._1.head.copy(categoryName = "updated category"))
          results <- dao.list(None, 10, None)
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
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    val beforeF = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.list(None, 10, None).map { before =>
        before shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
        before
      }
    }

    beforeF.flatMap { _ =>
      //when
      {
        for {
          insertId <- dao.insert(CategoryEntity(-1, "new category"))
          results <- dao.list(None, 10, None)
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

  it should "upsert existing record" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    val beforeF = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.list(None, 10, None).map { before =>
        before shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
        before
      }
    }

    beforeF.flatMap { before =>
      //when
      val entity = before._1.head
      val result = for {
        upserted <- dao.upsert(entity)
        results <- dao.list(None, 10, None)
      } yield {
        (upserted, results)
      }
      
      //then
      result.map { case (upserted, results) =>
        upserted shouldBe entity
        results shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
      }
    }
  }

  it should "upsert new record" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    val beforeF = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.list(None, 10, None).map { before =>
        before shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
        before
      }
    }

    beforeF.flatMap { _ =>
      //when
      val entity = CategoryEntity(-1, "new category")
      val result = for {
        upserted <- dao.upsert(entity)
        results <- dao.list(None, 10, None)
      } yield {
        (upserted, results)
      }
        
      //then
      result.map { case (upserted, results) =>
        upserted shouldBe entity.copy(id = 3)
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
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    val beforeF = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.list(None, 10, None).map { before =>
        before shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
        before
      }
    }

    beforeF.flatMap { _ =>
      //when
      {
        for {
          deleted <- dao.deleteAll()
          results <- dao.list(None, 10, None)
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
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    val beforeF = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.list(None, 10, None).map { before =>
        before shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
        before
      }
    }

    beforeF.flatMap { before =>
      //when
      {
        for {
          updated <- dao.updateMany(before._1.zipWithIndex.map { case (c, i) =>
            c.copy(categoryName = s"updated category $i")
          })
          results <- dao.list(None, 10, None)
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
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    val beforeF = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.list(None, 10, None).map { before =>
        before shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
        before
      }
    }

    beforeF.flatMap { _ =>
      //when
      {
        for {
          insertIds <- dao.insertMany(Seq(
            CategoryEntity(-1, "new category 1"),
            CategoryEntity(-1, "new category 2")
          ))
          results <- dao.list(None, 10, None)
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

  it should "rollback transaction when SQL error" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    val beforeF = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.list(None, 10, None).map { before =>
        before shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
        before
      }
    }

    beforeF.flatMap { before =>
      //when
      dao.ctx.performIO {
        dao.ctx.IO.sequence(Seq(
          dao.insertQuery(CategoryEntity(-1, "new category")),
          dao.insertQuery(before._1.head)
        ))
      }.failed.flatMap { error =>
        //then
        error.getMessage shouldBe {
          "Error: SQLITE_CONSTRAINT: UNIQUE constraint failed: categories.category_name"
        }
        dao.list(None, 10, None).map { after =>
          after shouldBe before
        }
      }
    }
  }
  
  ignore should "rollback transaction manually by throwing error" in {
    //given
    val db = WebSQL.openDatabase(":memory:")
    val dao = new CategoryDao(new ShowcaseDBContext(db))

    val beforeF = db.transaction { tx =>
      prepareDb(tx)
    }.flatMap { _ =>
      dao.list(None, 10, None).map { before =>
        before shouldBe {
          (Seq(
            CategoryEntity(1, "test category 1"),
            CategoryEntity(2, "test category 2")
          ), Some(2))
        }
        before
      }
    }

    beforeF.flatMap { before =>
      //when
      dao.ctx.performIO {
        for {
          _ <- dao.insertQuery(CategoryEntity(-1, "new category"))
        } yield {
          throw js.JavaScriptException(js.Error("test error"))
        }
      }.failed.flatMap { error =>
        //then
        error.getMessage shouldBe "test error"
        dao.list(None, 10, None).map { after =>
          after shouldBe before
        }
      }
    }
  }
}
