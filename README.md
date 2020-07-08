
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
  "org.scommons.websql" %%% "scommons-websql-migrations" % scommonsWebSqlVer,
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
        |  created_at      timestamp NOT NULL DEFAULT (strftime('%s','now') * 1000),
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

#### Setup DB migrations

To automate DB schema versioning you can use `scommons-websql-migrations`
library together with `sbt-scommons-plugin`.

First, set the `scommonsBundlesFileFilter` `sbt` build setting:
```sbt
import scommons.sbtplugin.ScommonsPlugin.autoImport._

...
scommonsBundlesFileFilter := "*.sql"
...
```

Then add your `SQL` migrations scripts:
* [V001__initial_db_structure.sql](migrations/src/test/resources/scommons/websql/migrations/V001__initial_db_structure.sql)
* [V002__rename_db_field.sql](migrations/src/test/resources/scommons/websql/migrations/V002__rename_db_field.sql)

This setup will automatically generate single `bundle.json` file
during the build with the all SQL scripts content inside.

Then you can read this file from the code:
```scala
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scommons.websql.migrations.WebSqlMigrationBundle

@js.native
@JSImport("./scommons/websql/migrations/bundle.json", JSImport.Namespace)
object TestMigrationsBundle extends WebSqlMigrationBundle
```

And run migrations at the start of your app:
```scala
import scommons.websql.migrations.WebSqlMigrations

val migrations = new WebSqlMigrations(db)

migrations.runBundle(TestMigrationsBundle)
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

  def deleteAll(): Future[Long] = {
    ctx.performIO(ctx.run(categories.delete))
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
business logic/service layer:

```scala
import showcase.domain.CategoryEntity
import showcase.domain.dao.CategoryDao

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
```

### How to Build

To build and run all the tests use the following command:
```bash
sbt test
```

## Documentation

You can find more documentation [here](https://scommons.org/)
