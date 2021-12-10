
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
  // see migrations/README.md
  "org.scommons.websql" %%% "scommons-websql-migrations" % scommonsWebSqlVer,

  // high level IO effect API (already includes core)
  "org.scommons.websql" %%% "scommons-websql-io" % scommonsWebSqlVer,
  
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

## Documentation

You can find more documentation [here](https://scommons.org/)
