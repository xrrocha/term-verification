package org.plenix.slick

import java.io.InputStream

import javax.sql.DataSource
import org.apache.commons.beanutils.BeanUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import slick.jdbc.JdbcProfile
object DSJdbcProfile extends JdbcProfile
import DSJdbcProfile.api._

// FIXME DatabaseSession creates separate connections for each implementation
trait DatabaseSession {
  def confInputStream: InputStream =
    getClass.getClassLoader.getResourceAsStream("database.properties")

  lazy val properties: Map[String, String] = {
    val properties = new java.util.Properties
    properties.load(confInputStream)

    import scala.collection.JavaConverters._
    properties.asScala.toMap
  }

  lazy val dataSource: DataSource = {
    val dataSourceClassName = properties("dataSource")
    val dataSource = Class.forName(dataSourceClassName).newInstance

    (properties - "dataSource").foreach {
      case (propertyName, propertyValue) =>
        BeanUtils.setProperty(dataSource, propertyName, propertyValue)
    }

    dataSource.asInstanceOf[javax.sql.DataSource]
  }

  lazy val db = Database.forDataSource(dataSource, Some(1))

  def select[A, B](q: Query[A, B, Seq]): Future[Seq[B]] = db.run(q.result)

  def selectOne[A, B](q: Query[A, B, Seq]): Future[B] = select(q).map(_.head)
}