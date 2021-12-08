package scommons.websql.encoding

case class MappedEncoding[I, O](f: I => O)
