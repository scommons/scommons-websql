
[![Build Status](https://travis-ci.org/scommons/scommons-websql.svg?branch=master)](https://travis-ci.org/scommons/scommons-websql)
[![Coverage Status](https://coveralls.io/repos/github/scommons/scommons-websql/badge.svg?branch=master)](https://coveralls.io/github/scommons/scommons-websql?branch=master)
[![scala-index](https://index.scala-lang.org/scommons/scommons-websql/scommons-websql-core/latest.svg)](https://index.scala-lang.org/scommons/scommons-websql/scommons-websql-core)
[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.17.svg)](https://www.scala-js.org)

## Scala Commons Web SQL
[Scala.js](https://www.scala-js.org) facade and bindings for [WebSQL API](https://www.w3.org/TR/webdatabase/)

It's relying on the following reference implementation:
https://github.com/nolanlawson/node-websql

This API can be backed by [SQLite](https://www.sqlite.org) on Node.js
and `react-native` platforms.

To use [quill](https://getquill.io) bindings include `scommons-websql-quill`
library.

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

//TODO

### How to Build

To build and run all the tests use the following command:
```bash
sbt test
```

## Documentation

You can find more documentation [here](https://scommons.org/)
