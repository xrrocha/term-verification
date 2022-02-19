package org.plenix.nomenclator.verification

import com.typesafe.scalalogging.LazyLogging
import org.plenix.slick.DSJdbcProfile.api._
import org.plenix.slick.{DSJdbcProfile, DatabaseSession}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait TestDatabaseSession extends DatabaseSession {
  override def confInputStream =
    getClass.getClassLoader.getResourceAsStream("test-database.properties")
}

trait VerificationSuite extends Suite with BeforeAndAfterAll with TestDatabaseSession with LazyLogging {

  import Model._

  private val schema: DSJdbcProfile.DDL = terms.schema ++ termUsages.schema ++ termSimilarities.schema ++ termNames.schema

  override def beforeAll() {
    db.run(DBIO.seq(schema.createIfNotExists))
  }

  override def afterAll() {
    db.run(DBIO.seq(schema.dropIfExists))
  }
}