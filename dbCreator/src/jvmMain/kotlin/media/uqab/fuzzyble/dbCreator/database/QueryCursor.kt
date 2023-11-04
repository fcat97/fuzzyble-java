package media.uqab.fuzzyble.dbCreator.database

import media.uqab.fuzzybleJava.SqlCursor
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

class QueryCursor(private val mConnection: Connection, private val mQuery: String) : SqlCursor {
    private lateinit var mStatement: Statement
    private var resultSet: ResultSet? = null

    init {
        try {
            mStatement = mConnection.createStatement()
            resultSet = mStatement.executeQuery(mQuery)
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    override fun getString(columnIndex: Int): String? {
        return try {
            resultSet?.getString(columnIndex + 1)
        } catch (e: SQLException) {
            println(e.message)
            null
        }
    }


    override fun moveToNext(): Boolean {
        return try {
            resultSet?.next() == true
        } catch (e: Exception) {
            println(e.message)
            false
        }
    }

    @Override
    public override fun count(): Int {
        var count = 0
        try {
            val st = mConnection.createStatement()
            val rs = st.executeQuery(mQuery)
            while (rs.next()) {
                count++
            }
            rs.close()
            st.close()
            println("count:$count q:$mQuery")
        } catch (e: Exception) {
            println("count-error: " + e.message)
        }

        return count
    }

    @Override
    override fun close() {
        try {
            resultSet?.close()
            mStatement.close()
        } catch (e: SQLException) {
            println(e.message)
        }
    }
}