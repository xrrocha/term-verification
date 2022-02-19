package org.plenix.nomenclator.verification

import com.typesafe.scalalogging.LazyLogging
import org.json4s.{DefaultFormats, Formats}
import org.plenix.slick.DatabaseSession
import org.scalatra.json._
import org.scalatra.{AsyncResult, FutureSupport, MethodOverride, ScalatraServlet}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object RESTServlet {
  val StartingTerm = "a"
  val TermCount = "16"

  val NameCount = "64"
  val SimilarityCount = "16"

  val MinLevenshtein = .71

  val LastModifiedCount = "8"

  case class Usage(usage: String, baseTermId: Option[String])

  case class Name(name: String)

  case class Similarity(termId: String, levenshtein: Double)

  case class TermResult(term: Term, usages: Seq[Usage], names: Seq[Name], similarities: Seq[Similarity], similarNames: Seq[Name])

  case class TermUpdate(term: Term, usages: Seq[Usage]) {
    def validate: Boolean =
      term.validate && (!term.termType.contains(Model.Name) || usages.nonEmpty)
  }

}

class RESTServlet
  extends ScalatraServlet
    with FutureSupport
    with MethodOverride
    with JacksonJsonSupport
    with DatabaseSession
    with LazyLogging {

  import Model._
  import RESTServlet._

  implicit val executor: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  get("/terms") {

    val start = Option(request.getParameter("start")).getOrElse(StartingTerm)
    val count = Option(request.getParameter("count")).getOrElse(TermCount).toInt

    new AsyncResult {
      val is: Future[Seq[Term]] = select {
        terms
          .filter(t => t.termType.inSet(requestedTermTypes) && t.termId >= start)
          .sortBy(_.termId)
          .take(count)
      }
    }
  }

  get("/terms", request.getParameterMap.containsKey("end")) {

    val end = Option(request.getParameter("end")).getOrElse("zzzzzzzz")
    val count = Option(request.getParameter("count")).getOrElse("32").toInt

    new AsyncResult {
      val is: Future[Seq[Term]] = select {
        terms.
          filter(t => t.termType.inSet(requestedTermTypes) && t.termId <= end).
          sortBy(_.termId.desc).
          take(count)
      }
        .map(_.sortBy(_.termId))
    }
  }

  get("/terms", request.getParameter("query") == "lastModified") {

    val count =
      Option(request.getParameter("count"))
        .getOrElse(LastModifiedCount)
        .toInt

    new AsyncResult {
      val is: Future[Seq[Term]] = select {
        terms.
          filter(t =>
            t.lastModified.isDefined && t.userModified === request.getRemoteUser
          ).
          sortBy(t => (t.lastModified.desc, t.termId.desc)).
          take(count)
      }
    }

  }

  get("/terms/:termId") {

    val termId = params("termId")
    val nameCount = Option(request.getParameter("nameCount")).getOrElse(NameCount).toInt
    // val similarityCount = Option(request.getParameter("similarityCount")).getOrElse(SimilarityCount).toInt

    new AsyncResult() {

      val is: Future[TermResult] =
        for {

          term <- selectOne {
            terms
              .filter(t => t.termId === termId)
          }

          usages <- select {
            termUsages
              .filter(tu => tu.termId === termId)
              .sortBy(_.usage)
          }

          names <- select {
            termNames
              .filter(tn => tn.termId === termId)
              .sortBy(_.name)
          }

          similarities <- select {
            termSimilarities
              .filter(ts => ts.term1 === termId) // && ts.levenshtein >= Minlevenshtein)
              .sortBy(_.levenshtein.desc)
          }

          similarNames <-
          if (similarities.isEmpty) {
            Future(Nil)
          }
          else {
            select {
              termNames
                .filter(tn => tn.termId === similarities.head.term2)
                .take(nameCount)
                .sortBy(_.name)
            }
          }

        } yield TermResult(
          term,
          usages.map(u => Usage(u.usage, u.baseTermId)),
          names.map(n => RESTServlet.Name(n.name)),
          similarities.map(s => Similarity(s.term2, s.levenshtein)),
          similarNames.map(sn => RESTServlet.Name(sn.name))
        )
    }
  }

  get("/terms/:termId/names") {

    val termId = params("termId")
    val nameCount = Option(request.getParameter("count")).getOrElse(NameCount).toInt

    new AsyncResult {
      val is: Future[Seq[RESTServlet.Name]] =
        for {
          termNames <- select {
            termNames
              .filter(tn => tn.termId === termId)
              .sortBy(_.name)
              .take(nameCount)
          }
        } yield termNames.map(tn => RESTServlet.Name(tn.name))
    }
  }

  put("/terms/:termId") {

    val (termId, term, termUpdate) =
      try {
        val termId = params("termId")

        val termUpdate = parse(request.body).extract[TermUpdate]
        val term = termUpdate.term

        if (!termUpdate.validate) {
          logger.warn("Term update fails validation")
          sys.error("Term update fails validation")
        }

        (termId, term, termUpdate)
      } catch {
        case t: Throwable =>
          t.printStackTrace()
          logger.warn(t.getMessage)
          throw t
      }

    new AsyncResult() {
      val is: Future[String] = {


        try {
          db.run {
            DBIO.seq(
              terms
                .filter(t => t.termId === termId)
                .update(
                  term.copy(
                    termType = term.termType,
                    baseTermId = term.baseTermId,
                    lastModified = Some(new java.sql.Timestamp(System.currentTimeMillis)),
                    userModified = Option(request.getRemoteUser)
                  )
                ),

              termUsages
                .filter(tu => tu.termId === termId)
                .delete,

              termUsages
                .filter(tu => tu.termId === termId)
                .delete,

              termUsages ++=
                termUpdate
                  .usages
                  .map(u => TermUsage(termId, u.usage, u.baseTermId))
            )
          }.map(_ => request.body)
        }
        catch {
          case t: Throwable =>
            t.printStackTrace()
            logger.warn(s"Error updating term: $t", t)
            throw t
        }
      }
    }
  }


  lazy val termTypeNames: Map[String, String] = Map(
    "name" -> "n",
    "nonName" -> "x",
    "typo" -> "t",
    "abbreviation" -> "a",
    "mixup" -> "m",
    "other" -> "o",
    "undef" -> "u"
  )

  def requestedTermTypes: Iterable[String] =
    termTypeNames
      .keys
      .filter(n => request.getParameter(n) == "true")
      .map(termTypeNames(_))

}
