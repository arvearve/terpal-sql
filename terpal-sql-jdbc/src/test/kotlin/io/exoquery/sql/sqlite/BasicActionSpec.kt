package io.exoquery.sql.sqlite

import io.exoquery.sql.TestDatabases
import io.exoquery.controller.jdbc.JdbcControllers
import io.exoquery.sql.Sql
import io.exoquery.controller.runOn
import io.exoquery.sql.run
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable

class BasicActionSpec : FreeSpec({

  val ds = TestDatabases.sqlite
  val ctx by lazy { JdbcControllers.Sqlite(ds)  }

  beforeEach {
    ds.run(
      """
      CREATE TABLE test(id INTEGER PRIMARY KEY AUTOINCREMENT); DROP TABLE test;
      DELETE FROM Person;
      DELETE FROM Address;
      """
    )
//    ds.run(
//      "UPDATE SQLITE_SEQUENCE SET SEQ=10 WHERE NAME='Person';"
//    )
  }

  @Serializable
  data class Person(val id: Int, val firstName: String, val lastName: String, val age: Int)

  val joe = Person(1, "Joe", "Bloggs", 111)
  val jim = Person(2, "Jim", "Roogs", 222)

  "Basic Insert" {
    Sql("INSERT INTO Person (id, firstName, lastName, age) VALUES (${joe.id}, ${joe.firstName}, ${joe.lastName}, ${joe.age})").action().runOn(ctx);
    Sql("INSERT INTO Person (id, firstName, lastName, age) VALUES (${jim.id}, ${jim.firstName}, ${jim.lastName}, ${jim.age})").action().runOn(ctx);
    Sql("SELECT id, firstName, lastName, age FROM Person").queryOf<Person>().runOn(ctx) shouldBe listOf(joe, jim)
  }

  "Insert Returning" {
    val id1 = Sql("INSERT INTO Person (firstName, lastName, age) VALUES (${joe.firstName}, ${joe.lastName}, ${joe.age}) RETURNING id").actionReturning<Int>().runOn(ctx);
    val id2 = Sql("INSERT INTO Person (firstName, lastName, age) VALUES (${jim.firstName}, ${jim.lastName}, ${jim.age}) RETURNING id").actionReturning<Int>().runOn(ctx);
    id1 shouldBe 1
    id2 shouldBe 2
    Sql("SELECT id, firstName, lastName, age FROM Person").queryOf<Person>().runOn(ctx) shouldBe listOf(joe, jim)
  }

  "Insert Returning Record" {
    val person1 = Sql("INSERT INTO Person (firstName, lastName, age) VALUES (${joe.firstName}, ${joe.lastName}, ${joe.age}) RETURNING id, firstName, lastName, age").actionReturning<Person>().runOn(ctx)
    val person2 = Sql("INSERT INTO Person (firstName, lastName, age) VALUES (${jim.firstName}, ${jim.lastName}, ${jim.age}) RETURNING id, firstName, lastName, age").actionReturning<Person>().runOn(ctx)
    person1 shouldBe joe
    person2 shouldBe jim
    Sql("SELECT id, firstName, lastName, age FROM Person").queryOf<Person>().runOn(ctx) shouldBe listOf(joe, jim)
  }

  "Insert Returning Ids" {
    val id1 = Sql("INSERT INTO Person (firstName, lastName, age) VALUES (${joe.firstName}, ${joe.lastName}, ${joe.age})").actionReturningId().runOn(ctx);
    val id2 = Sql("INSERT INTO Person (firstName, lastName, age) VALUES (${jim.firstName}, ${jim.lastName}, ${jim.age})").actionReturningId().runOn(ctx);
    id1 shouldBe 1
    id2 shouldBe 2
    Sql("SELECT id, firstName, lastName, age FROM Person").queryOf<Person>().runOn(ctx) shouldBe listOf(joe, jim)
  }

  "Insert Returning Ids - explicit" {
    val id1 = Sql("INSERT INTO Person (firstName, lastName, age) VALUES (${joe.firstName}, ${joe.lastName}, ${joe.age})").actionReturningId("id").runOn(ctx);
    val id2 = Sql("INSERT INTO Person (firstName, lastName, age) VALUES (${jim.firstName}, ${jim.lastName}, ${jim.age})").actionReturningId("id").runOn(ctx);
    id1 shouldBe 1
    id2 shouldBe 2
    Sql("SELECT id, firstName, lastName, age FROM Person").queryOf<Person>().runOn(ctx) shouldBe listOf(joe, jim)
  }
})
