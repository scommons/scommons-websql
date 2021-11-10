
[![Build Status](https://travis-ci.com/scommons/scommons-websql.svg?branch=master)](https://travis-ci.com/scommons/scommons-websql)
[![Coverage Status](https://coveralls.io/repos/github/scommons/scommons-websql/badge.svg?branch=master)](https://coveralls.io/github/scommons/scommons-websql?branch=master)
[![scala-index](https://index.scala-lang.org/scommons/scommons-websql/scommons-websql-core/latest-by-scala-version.svg?targetType=Js)](https://index.scala-lang.org/scommons/scommons-websql/scommons-websql-core)
[![Scala.js 0.6](https://www.scala-js.org/assets/badges/scalajs-0.6.29.svg)](https://www.scala-js.org)
[![Scala.js 1.0](https://www.scala-js.org/assets/badges/scalajs-1.1.0.svg)](https://www.scala-js.org)

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
  "org.scommons.websql" %%% "scommons-websql-migrations" % scommonsWebSqlVer,
  
  // optional, see quill/README.md
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

### How to Build

To build and run all the tests use the following command:
```bash
sbt test
```

## Documentation

You can find more documentation [here](https://scommons.org/)
