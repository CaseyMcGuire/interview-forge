package com.application.db

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.Properties
import java.util.UUID

@Testcontainers
class RuntimeConfigurationMigrationTest {
  private lateinit var testSchema: String
  private lateinit var dataSource: DriverManagerDataSource
  private lateinit var jdbc: JdbcTemplate

  @BeforeEach
  fun setUp() {
    testSchema = "runtime_${UUID.randomUUID().toString().replace("-", "")}"
    dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password).apply {
      connectionProperties = Properties().apply { setProperty("currentSchema", testSchema) }
    }
    migrateTo("6")
    jdbc = JdbcTemplate(dataSource)

    jdbc.update("INSERT INTO users (id, email, hashed_password) VALUES (1, 'runtime@example.com', 'test-only')")
    jdbc.update("INSERT INTO languages (key, display_name) VALUES ('python', 'Python')")
    jdbc.update("""
      INSERT INTO problems (id, slug, title, statement_markdown, difficulty, created_by_user_id)
      VALUES (1, 'first', 'First', 'First problem', 'EASY', 1),
             (2, 'second', 'Second', 'Second problem', 'EASY', 1)
    """.trimIndent())
    jdbc.update("""
      INSERT INTO problem_languages (id, problem_id, language_id, starter_code)
      SELECT p.id, p.id, l.id, 'starter code'
      FROM problems p CROSS JOIN languages l WHERE l.key = 'kotlin'
    """.trimIndent())
    jdbc.update("""
      INSERT INTO judge_configurations (problem_language_id, runtime, test_driver_code, time_limit_ms, memory_limit_mb)
      VALUES (1, 'kotlin-2.1', 'first driver', 1000, 256),
             (2, 'kotlin-2.2', 'second driver', 2000, 512)
    """.trimIndent())
    jdbc.update("""
      INSERT INTO submissions (user_id, problem_id, problem_language_id, runtime, source_code, kind, total_cases)
      VALUES (1, 1, 1, 'kotlin-older', 'submitted code', 'SUBMIT', 1)
    """.trimIndent())
  }

  @Test
  fun `migration removes runtime settings without changing judges or submission snapshots`() {
    migrateTo("7")

    assertEquals(
      listOf("first driver", "second driver"),
      jdbc.queryForList("SELECT test_driver_code FROM judge_configurations ORDER BY problem_language_id", String::class.java),
    )
    assertEquals(
      listOf(1000, 2000),
      jdbc.queryForList("SELECT time_limit_ms FROM judge_configurations ORDER BY problem_language_id", Int::class.java),
    )
    assertEquals(
      listOf(256, 512),
      jdbc.queryForList("SELECT memory_limit_mb FROM judge_configurations ORDER BY problem_language_id", Int::class.java),
    )
    assertEquals(0, jdbc.queryForObject("""
      SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = ? AND table_name IN ('languages', 'judge_configurations') AND column_name = 'runtime'
    """.trimIndent(), Int::class.java, testSchema))

    assertEquals("kotlin-older", jdbc.queryForObject("SELECT runtime FROM submissions", String::class.java))
  }

  private fun migrateTo(version: String) {
    Flyway.configure()
      .dataSource(dataSource)
      .schemas(testSchema)
      .target(version)
      .load()
      .migrate()
  }

  companion object {
    @Container
    @JvmStatic
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18.6-alpine"))
  }
}
