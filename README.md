
[![Build Status](https://travis-ci.org/scommons/scommons-websql.svg?branch=master)](https://travis-ci.org/scommons/scommons-websql)
[![Coverage Status](https://coveralls.io/repos/github/scommons/scommons-websql/badge.svg?branch=master)](https://coveralls.io/github/scommons/scommons-websql?branch=master)
[![scala-index](https://index.scala-lang.org/scommons/scommons-websql/scommons-websql-core/latest.svg)](https://index.scala-lang.org/scommons/scommons-websql/scommons-websql-core)
[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.17.svg)](https://www.scala-js.org)

## Scala Commons Web SQL
[Scala.js](https://www.scala-js.org) facade and [quill](https://getquill.io)
bindings for [WebSQL API](https://www.w3.org/TR/webdatabase/)

It's relying on the following reference implementation:
https://github.com/nolanlawson/node-websql

This API can be backed by [SQLite](https://www.sqlite.org) on Node.js
and `react-native` platforms.

### How to add it to your project

```scala
val scommonsWebSqlVer = "1.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scommons.websql" %%% "scommons-websql-core" % scommonsWebSqlVer,
  "org.scommons.websql" %%% "scommons-websql-quill" % scommonsWebSqlVer
)
```

Latest `SNAPSHOT` version is published to [Sonatype Repo](https://oss.sonatype.org/content/repositories/snapshots/org/scommons/), just make sure you added
the proper dependency resolver to your `build.sbt` settings:
```scala
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
```

### How to use it

#### Open Database

```scala
import scommons.websql.WebSQL

// on react-native (see scommons-expo module)
val db = SQLite.openDatabase("myfirst.db")

// on Node.js
val db = WebSQL.openDatabase("myfirst.db")

// or in-memory DB, useful for testing
val db = WebSQL.openDatabase(":memory:")
```

#### Create DB Schema

You can use `executeSql` method to run raw SQL queries:

```scala
db.transaction { tx =>
    
    tx.executeSql(
      """CREATE TABLE IF NOT EXISTS categories (
        |  id              integer primary key,
        |  category_name   text NOT NULL,
        |  created_at      timestamp without time zone DEFAULT current_timestamp,
        |  UNIQUE (category_name)
        |)
        |""".stripMargin
    )
    
    tx.executeSql(
      "INSERT INTO categories (category_name) VALUES (?), (?)",
      Seq(
        "test category 1",
        "test category 2"
      )
    )
}
```

#### Create quill DB Context

To use [quill](https://getquill.io) bindings include `scommons-websql-quill`
library.

[Example](quill/src/test/scala/showcase/domain/ShowcaseDBContext.scala)
quill context with pre-defined fields naming (`snake_case`):

```scala
import io.getquill.SnakeCase
import scommons.websql.Database
import scommons.websql.quill.SqliteContext

class ShowcaseDBContext(db: Database) extends SqliteContext(SnakeCase, db)
```

#### Create DB Entity

[Example](quill/src/test/scala/showcase/domain/CategoryEntity.scala)
entity class with DB schema definition:

```scala
case class CategoryEntity(id: Int,
                          categoryName: String)

trait CategorySchema {

  val ctx: ShowcaseDBContext
  import ctx._

  implicit val categoriesInsertMeta = insertMeta[CategoryEntity](
    _.id
  )
  implicit val categoriesUpdateMeta = updateMeta[CategoryEntity](
    _.id
  )

  val categories = quote(querySchema[CategoryEntity]("categories"))
}
```

#### Create DAO

[Example](quill/src/test/scala/showcase/domain/dao/CategoryDao.scala) DAO class with DB queries/actions:

```scala
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

  def update(entity: CategoryEntity)(implicit tx: Transaction): Future[Boolean] = {
    isUpdated(ctx.run(categories
      .filter(c => c.id == lift(entity.id))
      .update(lift(entity))
    ))
  }

  def deleteAll()(implicit tx: Transaction): Future[Int] = {
    ctx.run(categories.delete)
  }
}
```

#### Check generated SQL

Quill will log generated `SQL`s during compilation.
Example output (edited to fit on the screen):

```bash
/Users/me/showcase/domain/dao/CategoryDao.scala:16:30:
 SELECT c.id, c.category_name FROM categories c WHERE c.id = ?
    getOne("getById", ctx.run(categories
                             ^
/Users/me/showcase/domain/dao/CategoryDao.scala:22:12:
 SELECT COUNT(*) FROM categories x
    ctx.run(categories
           ^
/Users/me/showcase/domain/dao/CategoryDao.scala:37:27:
 SELECT COUNT(*) FROM categories c WHERE LOWER (c.category_name) like ?
      case None => ctx.run(categories
                          ^
/Users/me/showcase/domain/dao/CategoryDao.scala:42:29:
 SELECT x3.id, x3.category_name FROM categories x3
  WHERE LOWER (x3.category_name) like ?
   ORDER BY x3.category_name ASC NULLS FIRST LIMIT ? OFFSET ?
    val fetchQuery = ctx.run(categories
                            ^
/Users/me/showcase/domain/dao/CategoryDao.scala:57:12:
 INSERT INTO categories (category_name) VALUES (?)
    ctx.run(categories
           ^
/Users/me/showcase/domain/dao/CategoryDao.scala:76:22:
 UPDATE categories SET category_name = ? WHERE id = ?
    isUpdated(ctx.run(categories
                     ^
/Users/me/showcase/domain/dao/CategoryDao.scala:97:12:
 DELETE FROM categories
    ctx.run(categories.delete)
           ^
Done compiling.
```

#### Running queries

[Example](quill/src/test/scala/showcase/CategoryService.scala)
business logic service/layer:

```scala
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
```

### How to Build

To build and run all the tests use the following command:
```bash
sbt test
```

## Documentation

You can find more documentation [here](https://scommons.org/)
