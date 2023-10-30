package media.uqab.fuzzybleJava.impl;

import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import media.uqab.fuzzybleJava.SqlCursor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static media.uqab.fuzzybleJava.impl.Log.log;

public class QueryCursor implements SqlCursor {
    private Statement mStatement;
    private ResultSet resultSet = null;

    private final Connection mConnection;
    private final String mQuery;

    public QueryCursor(Connection connection, String query) {
        this.mConnection = connection;
        this.mQuery = query;

        try {
            mStatement = connection.createStatement();
            resultSet = mStatement.executeQuery(query);
        } catch (SQLException e) {
            log(e.getMessage());
        }
    }

    @Override
    public String getString(int columnIndex) {
        try {
            return resultSet.getString(columnIndex + 1);
        } catch (SQLException e) {
            log(e.getMessage());
            return null;
        }
    }

    @Override
    public boolean moveToNext() {
        try {
            return resultSet.next();
        } catch (Exception e) {
            log(e.getMessage());
            return false;
        }
    }

    @Override
    public int count() {
        int count = 0;
        try {
            Statement st = mConnection.createStatement();
            ResultSet rs = st.executeQuery(mQuery);
            while (rs.next()) {
                count++;
            }
            rs.close();
            st.close();
            log("count:" + count + " q:" + mQuery);
        } catch (Exception e) {
            log("count-error: "+ e.getMessage());
        }

        return count;
    }

    @Override
    public void close() {
        try {
            if (resultSet != null) resultSet.close();
            if (mStatement != null) mStatement.close();
        } catch (SQLException e) {
            log(e.getMessage());
        }
    }
}