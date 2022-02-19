package org.plenix.nomenclator.verification

import org.json4s.JsonAST.JArray
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Formats}
import org.plenix.slick.DSJdbcProfile.api._
import org.scalatest.FunSuite
import org.scalatra.test.scalatest.ScalatraSuite

import scala.concurrent.ExecutionContext

class TestRESTServlet extends RESTServlet with TestDatabaseSession

class RESTServletTest extends FunSuite with ScalatraSuite with VerificationSuite {

  import Model._
  import RESTServlet._

  implicit val jsonFormats: Formats = DefaultFormats

  implicit val executor: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val lastModified = Some(new java.sql.Timestamp(System.currentTimeMillis))

  val termRows: Seq[Term] = Seq(
    Term("maria", Some("n"), Some("maría")),
    Term("jose", Some("n"), Some("josé")),
    Term("ricardo", Some("n"), None, lastModified),
    Term("ricrddo", Some("t"), Some("ricardo")),
    Term("cigarrillo", Some("x")),
    Term("alex", Some("n"), None, lastModified)
  )

  val termUsageRows: Seq[TermUsage] = Seq(
    TermUsage("ricardo", "m"), TermUsage("ricardo", "s"),
    TermUsage("maria", "f"), TermUsage("maria", "M"),
    TermUsage("jose", "m"), TermUsage("jose", "F"),
    TermUsage("alex", "m"), TermUsage("alex", "f")
  )

  val termSimilarityRows: Seq[TermSimilarity] = Seq(
    TermSimilarity("ricardo", "ricrddo", 0.89, 0.74)
  )

  val termNameRows: Seq[TermName] = Seq(
    TermName("ricardo", "ricardo montalban"),
    TermName("ricardo", "ricky ricardo"),
    TermName("maria", "maria morena"),
    TermName("jose", "negro jose")
  )

  override def beforeAll() {
    super.beforeAll()

    addServlet(classOf[TestRESTServlet], "/*")
    start()

    db.run {
      DBIO.seq(
        terms ++= termRows,
        termUsages ++= termUsageRows,
        termSimilarities ++= termSimilarityRows,
        termNames ++= termNameRows
      )
    }
  }

  test("Retrieves multiple terms") {

    get("/terms") {

      status should equal(200)

      val json = parse(body)

      val termResults: Seq[Term] =
        json
          .asInstanceOf[JArray]
          .arr
          .map(_.extract[Term])
          .sortBy(_.termId)

      val sortedTerms: Seq[Term] = termRows.sortBy(_.termId)

      assert {
        termResults
          .zip(sortedTerms)
          .forall { case (termResult, term) => termResult.termId == term.termId }
      }
    }
  }

  test("Retrieves recently modified terms") {

    get("/terms", params = ("query", "lastModified")) {

      status should equal(200)

      val json = parse(body)
      val termResults: Seq[Term] =
        json
          .asInstanceOf[JArray]
          .arr
          .map(_.extract[Term])
          .sortBy(_.termId)

      assert(termResults.map(_.termId) == Seq("alex", "ricardo"))
    }
  }

  test("Retrieves single term") {

    get("/terms/ricardo") {

      status should equal(200)

      val termResult = parse(body).extract[TermResult]

      assert(termResult.term.termId == termRows(2).termId)
      assert(termResult.names.length == 2)
      assert(termResult.names.forall(_.name.contains("ricardo")))
      assert(termResult.usages.length == 2)
      assert(termResult.usages.map(_.usage).sorted.mkString == "ms")
      assert(termResult.similarities.length == 1)
      assert(termResult.similarities.head.termId == "ricrddo")
    }
  }

  test("Retrieves term names") {

    get("/terms/ricardo/names") {

      status should equal(200)

      val json = parse(body)
      val names = json.asInstanceOf[JArray].arr.map(_.extract[Name])

      assert(names.length == 2)
      assert(names.forall(_.name.contains("ricardo")))
    }
  }

  test("Saves term changes") {

    val body =
      """
        {
		  "term": {
		    "termId": "rubi",
		    "termType": "n"
		  },
		  "usages": [
		    { "usage": "f", "baseTermId": "rubi,rubí" },
		    { "usage": "s", "baseTermId": "rubí" }
		  ]
		}
    """

    put("/terms/alex", body.getBytes) {

      status should equal(200)
    }

    val term: Term = termRows(5)

    assert(term.termType.contains(Model.Name))

    import scala.concurrent._
    import scala.concurrent.duration._

    val usages: Future[Seq[String]] = select {
      termUsages
        .filter(tu => tu.termId === "alex")
        .map(_.usage)
    }
    logger.info(s"usages: $usages")

    val orderedUsages = Await.result(usages, 1.second).sorted
    logger.info(s"orderedUsages: $orderedUsages")

    assert(orderedUsages.mkString == "fm")
  }
}