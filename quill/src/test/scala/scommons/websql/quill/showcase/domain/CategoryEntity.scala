package scommons.websql.quill.showcase.domain

case class CategoryEntity(id: Int,
                          categoryName: String)

//noinspection TypeAnnotation
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
