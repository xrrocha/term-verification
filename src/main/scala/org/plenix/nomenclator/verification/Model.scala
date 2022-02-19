package org.plenix.nomenclator.verification

import java.sql.Timestamp

import slick.jdbc.PostgresProfile.api._

object Model {

  val terms = TableQuery[Terms]
  val termUsages = TableQuery[TermUsages]
  val termNames = TableQuery[TermNames]
  val termSimilarities = TableQuery[TermSimilarities]

  val Name = "n"
  val NonName = "x"
  val Abbreviation = "a"
  val Typo = "t"
  val WordMixup = "m"
  val Undefined = "u"

  def removeAccents(string: String): String = {
    val mappings = Map(
      'á' -> 'a',
      'é' -> 'e',
      'í' -> 'i',
      'ó' -> 'o',
      'ú' -> 'u')

    string.map { c => if (mappings.contains(c)) mappings(c) else c }
  }
}

case class Term(
                 termId: String,
                 termType: Option[String],
                 baseTermId: Option[String] = None,
                 lastModified: Option[Timestamp] = None,
                 userModified: Option[String] = None) {
  def validate: Boolean = !termType.contains(Model.Typo) || baseTermId.isDefined
}

class Terms(tag: Tag) extends Table[Term](tag, "terms") {
  def termId = column[String]("term_id", O.PrimaryKey)

  def termType = column[Option[String]]("term_type")

  def baseTermId = column[Option[String]]("base_term_id")

  def lastModified = column[Option[Timestamp]]("last_modified")

  def userModified = column[Option[String]]("user_modified")

  def * = (termId, termType, baseTermId, lastModified, userModified).mapTo[Term]

  def baseTermIdIdx = index("tr_base_term_id", baseTermId, unique = false)

  def lastModifiedIdx = index("tr_last_modified", lastModified, unique = false)
}

case class TermUsage(termId: String, usage: String, baseTermId: Option[String] = None)

class TermUsages(tag: Tag) extends Table[TermUsage](tag, "term_usages") {
  def termId = column[String]("term_id")

  def usage = column[String]("usage")

  def baseTermId = column[Option[String]]("base_term_id")

  def * = (termId, usage, baseTermId).mapTo[TermUsage]

  def pk = primaryKey("tu_pk", (termId, usage))

  def term = foreignKey("tu_term_fk", termId, TableQuery[Terms])(_.termId)
}

case class TermSimilarity(term1: String, term2: String, levenshtein: Double)

class TermSimilarities(tag: Tag) extends Table[TermSimilarity](tag, "term_similarities") {
  def term1 = column[String]("term_1")

  def term2 = column[String]("term_2")

  def levenshtein = column[Double]("levenshtein")

  def * = (term1, term2, levenshtein).mapTo[TermSimilarity]

  def term1Fk = foreignKey("ts_term1_fk", term1, TableQuery[Terms])(_.termId)

  def term2Fk = foreignKey("ts_term2_fk", term2, TableQuery[Terms])(_.termId)

  def pk = primaryKey("ts_pk", (term1, term2))
}

case class TermName(termId: String, name: String)

class TermNames(tag: Tag) extends Table[TermName](tag, "term_names") {
  def termId = column[String]("term_id")

  def name = column[String]("name")

  def * = (termId, name).mapTo[TermName]

  def term = foreignKey("tn_term_fk", termId, TableQuery[Terms])(_.termId)

  def pk = primaryKey("tn_pk", (termId, name))
}
