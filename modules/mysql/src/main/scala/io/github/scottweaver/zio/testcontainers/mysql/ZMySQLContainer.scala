package io.github.scottweaver.zio.testcontainers.mysql

import zio._
import com.dimafeng.testcontainers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import java.sql.Connection
import io.github.scottweaver.models.JdbcInfo
import javax.sql.DataSource
import com.mysql.cj.jdbc.MysqlDataSource
object ZMySQLContainer {

  final case class Settings(
    imageVersion: String,
    databaseName: String,
    username: String,
    password: String
  )

  object Settings {
    val default = ZLayer.succeed(
      Settings(
        "latest",
        MySQLContainer.defaultDatabaseName,
        MySQLContainer.defaultUsername,
        MySQLContainer.defaultPassword
      )
    )
  }

  type Provides = Has[JdbcInfo]
    with Has[Connection]
    with Has[Connection with AutoCloseable]
    with Has[DataSource]
    with Has[MySQLContainer]

  val live: ZLayer[Has[Settings], Nothing, Provides] = {

    def makeManagedConnection(container: MySQLContainer) =
      ZManaged.make(
        ZIO.effect {
          DriverManager.getConnection(
            container.jdbcUrl,
            container.username,
            container.password
          )
        }.orDie
      )(conn =>
        ZIO
          .effect(conn.close())
          .tapError(err => ZIO.effect(println(s"Error closing connection: $err")))
          .ignore
      )

    def makeManagedContainer(settings: Settings) =
      ZManaged.make(
        ZIO.effect {
          val containerDef = MySQLContainer.Def(
            dockerImageName = DockerImageName.parse(s"mysql:${settings.imageVersion}"),
            databaseName = settings.databaseName,
            username = settings.username,
            password = settings.password
          )
          containerDef.start()
        }.orDie
      )(container =>
        ZIO
          .effect(container.stop())
          .tapError(err => ZIO.effect(println(s"Error stopping container: $err")))
          .ignore
      )

    ZLayer.fromManagedMany {
      for {
        settings  <- ZIO.service[Settings].toManaged_
        container <- makeManagedContainer(settings)
        conn      <- makeManagedConnection(container)

      } yield {

        val jdbcInfo   = JdbcInfo(
          driverClassName = container.driverClassName,
          jdbcUrl = container.jdbcUrl,
          username = container.username,
          password = container.password
        )
        val dataSource = new MysqlDataSource()
        dataSource.setUrl(container.jdbcUrl)
        dataSource.setUser(container.username)
        dataSource.setPassword(container.password)

        Has(jdbcInfo) ++ Has(conn) ++ Has[DataSource](dataSource) ++ Has(container)
      }

    }
  }

}
