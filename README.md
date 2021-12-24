
[![Build Status](https://travis-ci.com/scommons/scommons-websql.svg?branch=master)](https://travis-ci.com/scommons/scommons-websql)
[![Coverage Status](https://coveralls.io/repos/github/scommons/scommons-websql/badge.svg?branch=master)](https://coveralls.io/github/scommons/scommons-websql?branch=master)
[![scala-index](https://index.scala-lang.org/scommons/scommons-websql/scommons-websql-core/latest.svg)](https://index.scala-lang.org/scommons/scommons-websql/scommons-websql-core)
[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-1.1.0.svg)](https://www.scala-js.org)

## Scala Commons Web SQL
[Scala.js](https://www.scala-js.org) facade for [WebSQL API](https://www.w3.org/TR/webdatabase/)

It's relying on the following reference implementation:
https://github.com/nolanlawson/node-websql

This API can be backed by [SQLite](https://www.sqlite.org) on Node.js
and `react-native` platforms.

### How to add it to your project

```scala
val scommonsWebSqlVer = "1.0.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scommons.websql" %%% "scommons-websql-core" % scommonsWebSqlVer,
  // see migrations/README.md
  "org.scommons.websql" %%% "scommons-websql-migrations" % scommonsWebSqlVer,

  // high level IO effect API (already includes core)
  "org.scommons.websql" %%% "scommons-websql-io" % scommonsWebSqlVer
)
```

Latest `SNAPSHOT` version is published to [Sonatype Repo](https://oss.sonatype.org/content/repositories/snapshots/org/scommons/), just make sure you added
the proper dependency resolver to your `build.sbt` settings:
```scala
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
```

### How to use it

#### Open Database

On `react-native` project (using
[scommons-expo](https://github.com/scommons/scommons-react-native#expo-modules)
module):
```scala
import scommons.expo.sqlite.SQLite

val db = SQLite.openDatabase("myfirst.db")
```

On `Node.js` project:
```scala
import scommons.websql.WebSQL

val db = WebSQL.openDatabase("myfirst.db")

// or in-memory DB, useful for testing
val db = WebSQL.openDatabase(":memory:")
```

#### Create DB Schema

You can use `tx.executeSql` method to run raw SQL queries:

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

It can be fully automated by using
[migrations](migrations/README.md) module.

#### Create DB Context

[Example](io/src/test/scala/scommons/websql/io/showcase/domain/ShowcaseDBContext.scala)
`SQLite` DB context:

```scala
import scommons.websql.Database
import scommons.websql.io.SqliteContext

class ShowcaseDBContext(db: Database) extends SqliteContext(db) {

  // example of custom encoder
  implicit val categoryIdToInt: MappedEncoding[CategoryId, Int] = mappedEncoding[CategoryId, Int](_.value)
  implicit val categoryIdEncoder: Encoder[CategoryId] = mappedEncoder[CategoryId, Int]

  // example of custom decoder
  implicit val intToCategoryId: MappedEncoding[Int, CategoryId] = mappedEncoding[Int, CategoryId](CategoryId)
  implicit val categoryIdDecoder: Decoder[CategoryId] = mappedDecoder[Int, CategoryId]
}
```

#### Create DB Entity

[Example](io/src/test/scala/scommons/websql/io/showcase/domain/CategoryEntity.scala)
DB `entity` class:

```scala
case class CategoryEntity(id: Int,
                          categoryName: String)
```

#### Create DAO

Data Access Object (`DAO`) layer has very similar query `IO` API
interface as [quill](quill/README.md), except that `SQL` has
to be written explicitly rather than generated during the build.

[Example 1](io/src/test/scala/scommons/websql/io/showcase/domain/dao/CategoryDao.scala)
`DAO` class with basic DB queries/actions:

```scala
import scommons.websql.io.dao.CommonDao
import scommons.websql.io.showcase.domain._

import scala.concurrent.Future

class CategoryDao(val ctx: ShowcaseDBContext) extends CommonDao {

  import ctx._

  def getByIdQuery(id: Int): IO[Seq[CategoryEntity], Effect.Read] = {
    ctx.runQuery(
      sql = "SELECT id, category_name FROM categories WHERE id = ?",
      args = id,
      extractor = CategoryEntity.tupled
    )
  }

  def getById(id: Int): Future[Option[CategoryEntity]] = {
    getOne("getById", ctx.performIO(getByIdQuery(id)))
  }

  def count(): Future[Int] = {
    ctx.performIO(
      ctx.runQuerySingle("SELECT count(*) FROM categories", identity[Int])
    )
  }

  def list(optOffset: Option[Int],
           limit: Int,
           symbols: Option[String]
          ): Future[(Seq[CategoryEntity], Option[Int])] = {

    val text = s"%${symbols.getOrElse("")}%"
    val offset = optOffset.getOrElse(0)

    val countQuery = optOffset match {
      case Some(_) => IO.successful(None)
      case None => ctx.runQuerySingle(
        sql = "SELECT count(*) FROM categories WHERE category_name LIKE ?",
        args = text,
        extractor = identity[Int]
      ).map(Some(_))
    }

    val fetchQuery = ctx.runQuery(
      sql =
        """SELECT
          |  id,
          |  category_name
          |FROM
          |  categories
          |WHERE
          |  category_name LIKE ?
          |ORDER BY
          |  category_name
          |LIMIT ?
          |OFFSET ?
          |""".stripMargin,
      args = (text, limit, offset),
      extractor = CategoryEntity.tupled
    )

    val q = for {
      maybeCount <- countQuery
      results <- fetchQuery
    } yield {
      (results, maybeCount)
    }

    // internally IO is always performed within transaction
    ctx.performIO(q)
  }

  def insertQuery(entity: CategoryEntity): IO[Int, Effect.Write] = {
    ctx.runActionReturning(
      "INSERT INTO categories (category_name) VALUES (?)", entity.categoryName
    ).map(_.toInt)
  }

  def insert(entity: CategoryEntity): Future[Int] = {
    ctx.performIO(insertQuery(entity))
  }

  def insertMany(list: Seq[CategoryEntity]): Future[Seq[Int]] = {
    ctx.performIO(IO.sequence(list.map(insertQuery)))
  }

  def upsert(entity: CategoryEntity): Future[CategoryEntity] = {
    val q = for {
      maybeCategory <- ctx.runQuery(
        sql = "SELECT id, category_name FROM categories WHERE category_name = ?",
        args = entity.categoryName,
        extractor = CategoryEntity.tupled
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
    ctx.runAction(
      sql = "UPDATE categories SET category_name = ? WHERE id = ?",
      args = (entity.categoryName, entity.id)
    )
  }

  def update(entity: CategoryEntity): Future[Boolean] = {
    isUpdated(ctx.performIO(updateQuery(entity)))
  }

  def updateMany(list: Seq[CategoryEntity]): Future[Seq[Boolean]] = {
    ctx.performIO(IO.sequence(list.map(updateQuery)).map { results =>
      results.map(_ > 0)
    })
  }

  def deleteAll(): Future[Long] = {
    ctx.performIO(ctx.runAction("DELETE FROM categories"))
  }
}
```

[Example 2](io/src/test/scala/scommons/websql/io/showcase/domain/dao/ProductDao.scala)
`DAO` class with more advanced DB queries:

```scala
import scommons.websql.io.dao.CommonDao
import scommons.websql.io.showcase.domain._

import scala.concurrent.Future

class ProductDao(val ctx: ShowcaseDBContext) extends CommonDao {

  import ctx._

  def allProducts(): Future[Seq[ProductEntity]] = {
    ctx.performIO(ctx.runQuery(
      "SELECT id, name, category_id FROM products ORDER BY id",
      ProductEntity.tupled
    ))
  }

  def joinProducts(): Future[Seq[(ProductEntity, CategoryEntity)]] = {
    ctx.performIO(ctx.runQuery(
      sql =
        """SELECT
          |  p.id             AS _0, -- ******************************* 
          |  p.name           AS _1, -- * NOTE:
          |  p.category_id    AS _2, -- *   for JOIN queries from different tables
          |  c.id             AS _3, -- *   ALWAYS specify custom unique fields names !!!
          |  c.category_name  AS _4  -- *******************************
          |FROM (
          |  SELECT id, name, category_id FROM products ORDER BY id
          |) AS p
          |INNER JOIN categories c ON p.category_id = c.id
          |""".stripMargin,
      extractor = { case (p, c) =>
        (ProductEntity.tupled(p), CategoryEntity.tupled(c))
      }: (((Int, String, Option[Int]), (Int, String))) =>
        (ProductEntity, CategoryEntity)
    ))
  }

  def leftJoinProducts(): Future[Seq[(ProductEntity, Option[CategoryEntity])]] = {
    ctx.performIO(ctx.runQuery(
      sql =
        """SELECT
          |  p.id             AS _0, -- ******************************* 
          |  p.name           AS _1, -- * NOTE:
          |  p.category_id    AS _2, -- *   for JOIN queries from different tables
          |  c.id             AS _3, -- *   ALWAYS specify custom unique fields names !!!
          |  c.category_name  AS _4  -- *******************************
          |FROM (
          |  SELECT id, name, category_id FROM products ORDER BY id
          |) AS p
          |LEFT JOIN categories c ON p.category_id = c.id
          |""".stripMargin,
      extractor = { case (p, c) =>
        (ProductEntity.tupled(p), c.map(CategoryEntity.tupled))
      }: (((Int, String, Option[Int]), Option[(Int, String)])) =>
        (ProductEntity, Option[CategoryEntity])
    ))
  }
}
```

#### Running queries

[Example](io/src/test/scala/scommons/websql/io/showcase/CategoryService.scala)
business logic / service layer:

```scala
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
```

## Documentation

You can find more documentation [here](https://scommons.org/)
