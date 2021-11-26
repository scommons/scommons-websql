
## scommons-websql-migrations

To automate DB schema versioning you can use `scommons-websql-migrations`
library together with [sbt-scommons-plugin](https://github.com/scommons/sbt-scommons-plugin)

First, set the `scommonsBundlesFileFilter` `sbt` build project
setting:
```sbt
import scommons.sbtplugin.ScommonsPlugin.autoImport._

project(...)
  .settings(
    ...,
    scommonsBundlesFileFilter := "*.sql",
    ...
  )
```

### SQL Migrations Scripts

Then add your `SQL` migrations scripts:
* [V001__initial_db_structure.sql](src/test/resources/scommons/websql/migrations/V001__initial_db_structure.sql)
* [V002__rename_db_field.sql](src/test/resources/scommons/websql/migrations/V002__rename_db_field.sql)

This setup will automatically generate single `bundle.json` file
during the build with the all SQL scripts content inside.

### Running Migrations

You can read generated `bundle.json` file from the code:
```scala
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scommons.websql.migrations.WebSqlMigrationBundle

@js.native
@JSImport("./scommons/websql/migrations/bundle.json", JSImport.Namespace)
object TestMigrationsBundle extends WebSqlMigrationBundle
```

This is how you can run migrations at the start of your app:
```scala
import scommons.websql.migrations.WebSqlMigrations

val migrations = new WebSqlMigrations(db)

migrations.runBundle(TestMigrationsBundle)
```
