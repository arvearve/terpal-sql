package io.exoquery.controller.jdbc

import io.exoquery.controller.JavaTimeEncoding
import io.exoquery.controller.*
import kotlinx.datetime.*
import java.math.BigDecimal
import java.sql.*
import java.time.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.*
import java.util.TimeZone
import kotlin.reflect.KClass

/** Represents a Jdbc encoder with a nullable or non-nulalble input value */
typealias JdbcEncoder<T> = SqlEncoder<Connection, PreparedStatement, T>

fun kotlinx.datetime.TimeZone.toJava(): TimeZone = TimeZone.getTimeZone(this.toJavaZoneId())

class JdbcEncoderAny<T: Any>(
  override val dataType: Int,
  override val type: KClass<T>,
  override val f: (JdbcEncodingContext, T, Int) -> Unit,
): EncoderAny<T, Int, Connection, PreparedStatement>(
  dataType, type,
  { i, stmt, dbType -> stmt.setNull(i, dbType) },
  f
)

open class JdbcBasicEncoding:
  BasicEncoding<Connection, PreparedStatement, ResultSet> {
  companion object: JdbcBasicEncoding()

  override val BooleanEncoder: JdbcEncoderAny<Boolean> = JdbcEncoderAny(Types.BOOLEAN, Boolean::class) { ctx, v, i -> ctx.stmt.setBoolean(i, v) }
  override val ByteEncoder: JdbcEncoderAny<Byte> = JdbcEncoderAny(Types.TINYINT, Byte::class) { ctx, v, i -> ctx.stmt.setByte(i, v) }
  override val CharEncoder: JdbcEncoderAny<Char> = JdbcEncoderAny(Types.VARCHAR, Char::class) { ctx, v, i -> ctx.stmt.setString(i, v.toString()) }
  override val DoubleEncoder: JdbcEncoderAny<Double> = JdbcEncoderAny(Types.DOUBLE, Double::class) { ctx, v, i -> ctx.stmt.setDouble(i, v) }
  override val FloatEncoder: JdbcEncoderAny<Float> = JdbcEncoderAny(Types.FLOAT, Float::class) { ctx, v, i -> ctx.stmt.setFloat(i, v) }
  override val IntEncoder: JdbcEncoderAny<Int> = JdbcEncoderAny(Types.INTEGER, Int::class) { ctx, v, i -> ctx.stmt.setInt(i, v) }
  override val LongEncoder: JdbcEncoderAny<Long> = JdbcEncoderAny(Types.BIGINT, Long::class) { ctx, v, i -> ctx.stmt.setLong(i, v) }
  override val ShortEncoder: JdbcEncoderAny<Short> = JdbcEncoderAny(Types.SMALLINT, Short::class) { ctx, v, i -> ctx.stmt.setShort(i, v) }
  override val StringEncoder: JdbcEncoderAny<String> = JdbcEncoderAny(Types.VARCHAR, String::class) { ctx, v, i -> ctx.stmt.setString(i, v) }
  override val ByteArrayEncoder: JdbcEncoderAny<ByteArray> = JdbcEncoderAny(Types.VARBINARY, ByteArray::class) { ctx, v, i -> ctx.stmt.setBytes(i, v) }


  override fun preview(index: Int, row: ResultSet): String? = row.getObject(index)?.let { it.toString() }
  override fun isNull(index: Int, row: ResultSet): Boolean {
    row.getObject(index)
    return row.wasNull()
  }

  override val BooleanDecoder: JdbcDecoderAny<Boolean> = JdbcDecoderAny(Boolean::class) { ctx, i -> ctx.row.getBoolean(i) }
  override val ByteDecoder: JdbcDecoderAny<Byte> = JdbcDecoderAny(Byte::class) { ctx, i -> ctx.row.getByte(i) }
  override val CharDecoder: JdbcDecoderAny<Char> = JdbcDecoderAny(Char::class) { ctx, i -> ctx.row.getString(i)?.let { it[0] } ?: Char.MIN_VALUE }
  override val DoubleDecoder: JdbcDecoderAny<Double> = JdbcDecoderAny(Double::class) { ctx, i -> ctx.row.getDouble(i) }
  override val FloatDecoder: JdbcDecoderAny<Float> = JdbcDecoderAny(Float::class) { ctx, i -> ctx.row.getFloat(i) }
  override val IntDecoder: JdbcDecoderAny<Int> = JdbcDecoderAny(Int::class) { ctx, i -> ctx.row.getInt(i) }
  override val LongDecoder: JdbcDecoderAny<Long> = JdbcDecoderAny(Long::class) { ctx, i -> ctx.row.getLong(i) }
  override val ShortDecoder: JdbcDecoderAny<Short> = JdbcDecoderAny(Short::class) { ctx, i -> ctx.row.getShort(i) }
  override val StringDecoder: JdbcDecoderAny<String> = JdbcDecoderAny(String::class) { ctx, i -> ctx.row.getString(i) }
  override val ByteArrayDecoder: JdbcDecoderAny<ByteArray> = JdbcDecoderAny(ByteArray::class) { ctx, i -> ctx.row.getBytes(i) }
}

object AdditionalPostgresEncoding {
  val SqlJsonEncoder: JdbcEncoder<SqlJson> = JdbcEncoderAny(Types.OTHER, SqlJson::class) { ctx, v, i -> ctx.stmt.setObject(i, v.value, Types.OTHER) }
  val SqlJsonDecoder: JdbcDecoder<SqlJson> = JdbcDecoderAny(SqlJson::class) { ctx, i -> SqlJson(ctx.row.getString(i)) }

  val encoders = setOf(SqlJsonEncoder)
  val decoders = setOf(SqlJsonDecoder)
}

object AdditionalJdbcEncoding {
  val BigDecimalEncoder: JdbcEncoderAny<BigDecimal> = JdbcEncoderAny(Types.NUMERIC, BigDecimal::class) { ctx, v, i -> ctx.stmt.setBigDecimal(i, v) }
  val BigDecimalDecoder: JdbcDecoderAny<BigDecimal> = JdbcDecoderAny(BigDecimal::class) { ctx, i -> ctx.row.getBigDecimal(i) }

  val SqlDateEncoder: JdbcEncoderAny<java.sql.Date> = JdbcEncoderAny(Types.DATE, java.sql.Date::class) { ctx, v, i -> ctx.stmt.setDate(i, v) }
  val SqlTimeEncoder: JdbcEncoderAny<Time> = JdbcEncoderAny(Types.TIME, Time::class) { ctx, v, i -> ctx.stmt.setTime(i, v) }
  val SqlTimestampEncoder: JdbcEncoderAny<Timestamp> = JdbcEncoderAny(Types.TIMESTAMP, Timestamp::class) { ctx, v, i -> ctx.stmt.setTimestamp(i, v) }
  val SqlDateDecoder: JdbcDecoderAny<java.sql.Date> = JdbcDecoderAny(java.sql.Date::class) { ctx, i -> ctx.row.getDate(i) }
  val SqlTimeDecoder: JdbcDecoderAny<Time> = JdbcDecoderAny(Time::class) { ctx, i -> ctx.row.getTime(i) }
  val SqlTimestampDecoder: JdbcDecoderAny<Timestamp> = JdbcDecoderAny(Timestamp::class) { ctx, i -> ctx.row.getTimestamp(i) }

  val encoders = setOf(BigDecimalEncoder, SqlDateEncoder, SqlTimeEncoder, SqlTimestampEncoder)
  val decoders = setOf(BigDecimalDecoder, SqlDateDecoder, SqlTimeDecoder, SqlTimestampDecoder)
}

open class JdbcTimeEncoding: JavaTimeEncoding<Connection, PreparedStatement, ResultSet> {
  override val JDateEncoder: JdbcEncoderAny<java.util.Date> = JdbcEncoderAny(Types.TIMESTAMP, java.util.Date::class) { ctx, v, i ->
    ctx.stmt.setTimestamp(i, Timestamp(v.getTime()), Calendar.getInstance(ctx.timeZone.toJava()))
  }
  override val JDateDecoder: JdbcDecoderAny<java.util.Date> = JdbcDecoderAny(java.util.Date::class) { ctx, i ->
    java.util.Date(ctx.row.getTimestamp(i, Calendar.getInstance(ctx.timeZone.toJava())).getTime())
  }

  // Encoders
  open val jdbcTypeOfLocalDate     = Types.DATE
  open val jdbcTypeOfLocalTime     = Types.TIME
  open val jdbcTypeOfLocalDateTime = Types.TIMESTAMP
  open val jdbcTypeOfZonedDateTime = Types.TIMESTAMP_WITH_TIMEZONE
  open val jdbcTypeOfInstant                      = Types.TIMESTAMP_WITH_TIMEZONE
  open val jdbcTypeOfOffsetTime                   = Types.TIME_WITH_TIMEZONE
  open val jdbcTypeOfOffsetDateTime               = Types.TIMESTAMP_WITH_TIMEZONE
  open val timezone: TimeZone = TimeZone.getDefault()
  open fun jdbcEncodeInstant(value: Instant): Any = value.atOffset(ZoneOffset.UTC)

  // Encoders for the KMP datetimes
  override val LocalDateEncoder: JdbcEncoderAny<kotlinx.datetime.LocalDate> = JdbcEncoderAny(jdbcTypeOfLocalDate, kotlinx.datetime.LocalDate::class) { ctx, v, i -> ctx.stmt.setObject(i, v.toJavaLocalDate(), jdbcTypeOfLocalDate) }
  override val LocalDateTimeEncoder: JdbcEncoderAny<kotlinx.datetime.LocalDateTime> = JdbcEncoderAny(jdbcTypeOfLocalDateTime, kotlinx.datetime.LocalDateTime::class) { ctx, v, i -> ctx.stmt.setObject(i, v.toJavaLocalDateTime(), jdbcTypeOfLocalDateTime) }
  override val LocalTimeEncoder: JdbcEncoderAny<kotlinx.datetime.LocalTime> = JdbcEncoderAny(jdbcTypeOfLocalTime, kotlinx.datetime.LocalTime::class) { ctx, v, i -> ctx.stmt.setObject(i, v.toJavaLocalTime(), jdbcTypeOfLocalTime) }
  override val InstantEncoder: JdbcEncoderAny<kotlinx.datetime.Instant> = JdbcEncoderAny(jdbcTypeOfInstant, kotlinx.datetime.Instant::class) { ctx, v, i -> ctx.stmt.setObject(i, jdbcEncodeInstant(v.toJavaInstant()), jdbcTypeOfInstant) }

  override val JLocalDateEncoder: JdbcEncoderAny<LocalDate> = JdbcEncoderAny(jdbcTypeOfLocalDate, LocalDate::class) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfLocalDate) }
  override val JLocalTimeEncoder: JdbcEncoderAny<LocalTime> = JdbcEncoderAny(jdbcTypeOfLocalTime, LocalTime::class) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfLocalTime) }
  override val JLocalDateTimeEncoder: JdbcEncoderAny<LocalDateTime> = JdbcEncoderAny(jdbcTypeOfLocalDateTime, LocalDateTime::class) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfLocalDateTime) }
  override val JZonedDateTimeEncoder: JdbcEncoderAny<ZonedDateTime> = JdbcEncoderAny(jdbcTypeOfZonedDateTime, ZonedDateTime::class) { ctx, v, i -> ctx.stmt.setObject(i, v.toOffsetDateTime(), jdbcTypeOfZonedDateTime) }

  override val JInstantEncoder: JdbcEncoderAny<Instant> = JdbcEncoderAny(jdbcTypeOfInstant, Instant::class) { ctx, v, i -> ctx.stmt.setObject(i, jdbcEncodeInstant(v), jdbcTypeOfInstant) }
  override val JOffsetTimeEncoder: JdbcEncoderAny<OffsetTime> = JdbcEncoderAny(jdbcTypeOfOffsetTime, OffsetTime::class) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfOffsetTime) }
  override val JOffsetDateTimeEncoder: JdbcEncoderAny<OffsetDateTime> = JdbcEncoderAny(jdbcTypeOfOffsetDateTime, OffsetDateTime::class) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfOffsetDateTime) }

  // Decoders for the KMP datetimes
  override val LocalDateDecoder: JdbcDecoderAny<kotlinx.datetime.LocalDate> = JdbcDecoderAny(kotlinx.datetime.LocalDate::class) { ctx, i -> ctx.row.getObject(i, LocalDate::class.java).toKotlinLocalDate() }
  override val LocalDateTimeDecoder: JdbcDecoderAny<kotlinx.datetime.LocalDateTime> = JdbcDecoderAny(kotlinx.datetime.LocalDateTime::class) { ctx, i -> ctx.row.getObject(i, LocalDateTime::class.java).toKotlinLocalDateTime() }
  override val LocalTimeDecoder: JdbcDecoderAny<kotlinx.datetime.LocalTime> = JdbcDecoderAny(kotlinx.datetime.LocalTime::class) { ctx, i -> ctx.row.getObject(i, LocalTime::class.java).toKotlinLocalTime() }
  override val InstantDecoder: JdbcDecoderAny<kotlinx.datetime.Instant> = JdbcDecoderAny(kotlinx.datetime.Instant::class) { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java).toInstant().toKotlinInstant() }

  // Decoders
  override val JLocalDateDecoder: JdbcDecoderAny<LocalDate> = JdbcDecoderAny(LocalDate::class) { ctx, i -> ctx.row.getObject(i, LocalDate::class.java) }
  override val JLocalTimeDecoder: JdbcDecoderAny<LocalTime> = JdbcDecoderAny(LocalTime::class) { ctx, i -> ctx.row.getObject(i, LocalTime::class.java) }
  override val JLocalDateTimeDecoder: JdbcDecoderAny<LocalDateTime> = JdbcDecoderAny(LocalDateTime::class) { ctx, i -> ctx.row.getObject(i, LocalDateTime::class.java) }
  override val JZonedDateTimeDecoder: JdbcDecoderAny<ZonedDateTime> = JdbcDecoderAny(ZonedDateTime::class) { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java).toZonedDateTime() }

  override val JInstantDecoder: JdbcDecoderAny<Instant> = JdbcDecoderAny(Instant::class) { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java).toInstant() }
  override val JOffsetTimeDecoder: JdbcDecoderAny<OffsetTime> = JdbcDecoderAny(OffsetTime::class) { ctx, i -> ctx.row.getObject(i, OffsetTime::class.java) }
  override val JOffsetDateTimeDecoder: JdbcDecoderAny<OffsetDateTime> = JdbcDecoderAny(OffsetDateTime::class) { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java) }
}

object JdbcTimeEncodingLegacy: JavaTimeEncoding<Connection, PreparedStatement, ResultSet> {
  override val JDateEncoder: JdbcEncoderAny<java.util.Date> = JdbcEncoderAny(Types.TIMESTAMP, java.util.Date::class) { ctx, v, i ->
    ctx.stmt.setTimestamp(i, Timestamp(v.getTime()), Calendar.getInstance(ctx.timeZone.toJava()))
  }
  override val JDateDecoder: JdbcDecoderAny<java.util.Date> = JdbcDecoderAny(java.util.Date::class) { ctx, i ->
    java.util.Date(ctx.row.getTimestamp(i, Calendar.getInstance(ctx.timeZone.toJava())).getTime())
  }

  // KMP Date Encoders
  override val LocalDateEncoder: JdbcEncoderAny<kotlinx.datetime.LocalDate> = JdbcEncoderAny(Types.DATE, kotlinx.datetime.LocalDate::class) { ctx, v, i -> ctx.stmt.setDate(i, java.sql.Date.valueOf(v.toJavaLocalDate())) }
  override val LocalDateTimeEncoder: JdbcEncoderAny<kotlinx.datetime.LocalDateTime> = JdbcEncoderAny(Types.TIMESTAMP, kotlinx.datetime.LocalDateTime::class) { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.valueOf(v.toJavaLocalDateTime())) }
  override val LocalTimeEncoder: JdbcEncoderAny<kotlinx.datetime.LocalTime> = JdbcEncoderAny(Types.TIME, kotlinx.datetime.LocalTime::class) { ctx, v, i -> ctx.stmt.setTime(i, Time.valueOf(v.toJavaLocalTime())) }
  override val InstantEncoder: JdbcEncoderAny<kotlinx.datetime.Instant> = JdbcEncoderAny(Types.TIMESTAMP, kotlinx.datetime.Instant::class) { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.from(v.toJavaInstant())) }

  override val JLocalDateEncoder: JdbcEncoderAny<LocalDate> = JdbcEncoderAny(Types.DATE, LocalDate::class) { ctx, v, i -> ctx.stmt.setDate(i, java.sql.Date.valueOf(v)) }
  override val JLocalTimeEncoder: JdbcEncoderAny<LocalTime> = JdbcEncoderAny(Types.TIME, LocalTime::class) { ctx, v, i -> ctx.stmt.setTime(i, Time.valueOf(v)) }
  override val JLocalDateTimeEncoder: JdbcEncoderAny<LocalDateTime> = JdbcEncoderAny(Types.TIMESTAMP, LocalDateTime::class) { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.valueOf(v)) }
  override val JZonedDateTimeEncoder: JdbcEncoderAny<ZonedDateTime> = JdbcEncoderAny(Types.TIMESTAMP, ZonedDateTime::class) { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.from(v.toInstant())) }
  override val JInstantEncoder: JdbcEncoderAny<Instant> = JdbcEncoderAny(Types.TIMESTAMP, Instant::class) { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.from(v)) }
  override val JOffsetTimeEncoder: JdbcEncoderAny<OffsetTime> = JdbcEncoderAny(Types.TIME, OffsetTime::class) { ctx, v, i -> ctx.stmt.setTime(i, Time.valueOf(v.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime()))  }
  override val JOffsetDateTimeEncoder: JdbcEncoderAny<OffsetDateTime> = JdbcEncoderAny(Types.TIMESTAMP, OffsetDateTime::class) { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.from(v.toInstant())) }

  // KMP Date Decoders
  override val LocalDateDecoder: JdbcDecoderAny<kotlinx.datetime.LocalDate> = JdbcDecoderAny(kotlinx.datetime.LocalDate::class) { ctx, i -> ctx.row.getDate(i).toLocalDate().toKotlinLocalDate() }
  override val LocalDateTimeDecoder: JdbcDecoderAny<kotlinx.datetime.LocalDateTime> = JdbcDecoderAny(kotlinx.datetime.LocalDateTime::class) { ctx, i -> ctx.row.getTimestamp(i).toLocalDateTime().toKotlinLocalDateTime() }
  override val LocalTimeDecoder: JdbcDecoderAny<kotlinx.datetime.LocalTime> = JdbcDecoderAny(kotlinx.datetime.LocalTime::class) { ctx, i -> ctx.row.getTime(i).toLocalTime().toKotlinLocalTime() }
  override val InstantDecoder: JdbcDecoderAny<kotlinx.datetime.Instant> = JdbcDecoderAny(kotlinx.datetime.Instant::class) { ctx, i -> ctx.row.getTimestamp(i).toInstant().toKotlinInstant() }

  override val JLocalDateDecoder: JdbcDecoderAny<LocalDate> = JdbcDecoderAny(LocalDate::class) { ctx, i -> ctx.row.getDate(i).toLocalDate() }
  override val JLocalTimeDecoder: JdbcDecoderAny<LocalTime> = JdbcDecoderAny(LocalTime::class) { ctx, i -> ctx.row.getTime(i).toLocalTime() }
  override val JLocalDateTimeDecoder: JdbcDecoderAny<LocalDateTime> = JdbcDecoderAny(LocalDateTime::class) { ctx, i -> ctx.row.getTimestamp(i).toLocalDateTime() }
  override val JZonedDateTimeDecoder: JdbcDecoderAny<ZonedDateTime> = JdbcDecoderAny(ZonedDateTime::class) { ctx, i -> ZonedDateTime.ofInstant(ctx.row.getTimestamp(i).toInstant(), ctx.timeZone.toJavaZoneId()) }
  override val JInstantDecoder: JdbcDecoderAny<Instant> = JdbcDecoderAny(Instant::class) { ctx, i -> ctx.row.getTimestamp(i).toInstant() }
  override val JOffsetTimeDecoder: JdbcDecoderAny<OffsetTime> = JdbcDecoderAny(OffsetTime::class) { ctx, i -> OffsetTime.of(ctx.row.getTime(i).toLocalTime(), ZoneOffset.UTC) }
  override val JOffsetDateTimeDecoder: JdbcDecoderAny<OffsetDateTime> = JdbcDecoderAny(OffsetDateTime::class) { ctx, i -> OffsetDateTime.ofInstant(ctx.row.getTimestamp(i).toInstant(), ctx.timeZone.toJavaZoneId()) }
}
