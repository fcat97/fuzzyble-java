package media.uqab.fuzzyble.dbCreator.database

import media.uqab.fuzzybleJava.Fuzzyble
import media.uqab.fuzzybleJava.SqlCursor
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class Database(url: String): Fuzzyble {
    private val mConnection: Connection

    init {
        Class.forName("org.sqlite.JDBC")
        mConnection = DriverManager.getConnection("jdbc:sqlite:$url")
    }

    override fun onQuery(p0: String): SqlCursor {
        return QueryCursor(mConnection, p0)
    }

    override fun onQuery(p0: String, p1: Array<String>?): SqlCursor {
        return QueryCursor(mConnection, prepareQuery(p0, p1))
    }

    override fun onExecute(p0: String, p1: Array<String>?) {
        val stmt = mConnection.createStatement()
        if (p1 == null) {
            stmt.executeUpdate(p0)
        } else {
            stmt.executeUpdate(prepareQuery(p0, p1))
        }
        stmt.close()
    }

    fun getTables(): List<String> {
        val cursor = "SELECT * FROM sqlite_master where type='table';".query() ?: return emptyList()
        val result = mutableListOf<String>()
        while (cursor.next()) {
            result.add(cursor.getString(2))
        }
        cursor.close()
        cursor.statement.close()
        return result
    }

    fun getColumns(tableName: String): List<String> {
        val result = mutableListOf<String>()
        val rs: ResultSet = "select * from $tableName LIMIT 0".query() ?: return result
        val mrs = rs.metaData
        for (i in 1..mrs.columnCount) {
            val row = arrayOfNulls<Any>(3)
            row[0] = mrs.getColumnLabel(i)
            row[1] = mrs.getColumnTypeName(i)
            row[2] = mrs.getPrecision(i)

            result.add(row[0].toString())
        }
        rs.close()
        rs.statement.close()
        return result
    }

    fun getFirst100Row(tableName: String): List<String> {
        val result = mutableListOf<String>()
        val columns = getColumns(tableName)
        val rs: ResultSet = "select * from $tableName LIMIT 100".query() ?: return result
        while (rs.next()) {
            val row = mutableListOf<String>()
            for (i in 1 .. columns.size) {
                val v: String? = when(rs.metaData.getColumnTypeName(i)) {
                    "INTEGER" -> rs.getInt(i).toString()
                    "TEXT" -> rs.getString(i)
                    else -> rs.getString(i)
                }
                if (v != null) {
                    row.add(v)
                } else {
                    row.add("null")
                }
            }
            result.add(row.joinToString(separator = SEPARATOR))
        }
        rs.close()
        rs.statement.close()
        return result
    }

    fun getData(tableName: String, columnName: String): List<String> {
        val result = mutableListOf<String>()
        val rs: ResultSet = "select $columnName from $tableName".query() ?: return result
        while (rs.next()) {
            val s = rs.getString(columnName)
            result.add(s)
        }
        rs.close()
        rs.statement.close()
        return result
    }

    fun searchItems(tableName: String, columnName: String, search: String): List<String> {
        val result = mutableListOf<String>()
        val columns = getColumns(tableName)
        val rs: ResultSet = "select * from $tableName where $columnName LIKE '%$search%' LIMIT 100".query() ?: return result
        var c = 0
        while (rs.next()) {
            val row = mutableListOf<String>()
            for (i in 1 .. columns.size) {
                val v: String? = when(rs.metaData.getColumnTypeName(i)) {
                    "INTEGER" -> rs.getInt(i).toString()
                    "TEXT" -> rs.getString(i)
                    else -> rs.getString(i)
                }
                if (v != null) {
                    row.add(v)
                } else {
                    row.add("null")
                }
            }
            c++
            result.add(row.joinToString(separator = SEPARATOR))
        }
        println("found $c items for $search")
        rs.close()
        rs.statement.close()
        return result
    }

    private fun String.query(): ResultSet? {

        val stmt = mConnection.createStatement()
        return stmt.executeQuery(this)
    }

    private fun prepareQuery(query: String, args: Array<String>?): String {
        if (args == null) return query
        var mutQuery = query
        for (a in args) {
            mutQuery = mutQuery.replaceFirst("\\?".toRegex(), "'$a'")
        }
        return mutQuery
    }

    fun close() {
        if(!mConnection.isClosed) {
            mConnection.close()
        }
    }

    companion object {
        const val SEPARATOR = "-:-"
    }
}