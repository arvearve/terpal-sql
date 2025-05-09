package io.exoquery.sql.examples

import io.exoquery.sql.Sql
import io.exoquery.controller.jdbc.JdbcControllers
import io.exoquery.controller.runOn
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ContextualColumn {

  object DateAsStringSerialzier: KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.format(DateTimeFormatter.ISO_DATE))
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.from(DateTimeFormatter.ISO_DATE.parse(decoder.decodeString()))
  }

  @Serializable
  data class Customer(val firstName: String, val lastName: String)

  suspend fun main() {
    val postgres = EmbeddedPostgres.start()
    postgres.run("CREATE TABLE customers (id SERIAL PRIMARY KEY, first_name TEXT, last_name TEXT)")
    val ctx = JdbcControllers.Postgres(postgres.postgresDatabase)

    //Sql("INSERT INTO customers (first_name, last_name, created_at) VALUES (${id("Alice" + i)}, ${id("Smith" + 1)}, ${id(LocalDate.of(2021, 1, 1))})").action().runOn(ctx)

    // 40ms
    Sql("INSERT INTO customers (id, first_name, last_name) SELECT gs, 'Alice-'||gs, 'Smith-'||gs FROM generate_series(1, 1000000) AS gs;").action().runOn(ctx)

    //INSERT INTO tableName (id) SELECT * FROM generate_series(1, 1000);

    // 10ms
    val customers = Sql("SELECT first_name, last_name FROM customers").queryOf<Customer>().runOn(ctx)

    println(customers.size)
  }
}

suspend fun main() {
  ContextualColumn.main()
}
