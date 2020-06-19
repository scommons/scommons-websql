package showcase.domain

import io.getquill.SnakeCase
import scommons.websql.Database
import scommons.websql.quill.SqliteContext

class ShowcaseDBContext(db: Database) extends SqliteContext(SnakeCase, db)
