package com.github.fCat97.database;

import media.uqab.fuzzybleJava.SqlCursor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryCursor implements SqlCursor {
    private final Connection mConnection;
    private final String mQuery;
    private Statement mStatement;
    private ResultSet resultSet;

    public QueryCursor(Connection connection, String query) {
        this.mConnection = connection;
        this.mQuery = query;
        try {
            this.mStatement = this.mConnection.createStatement();
            this.resultSet = this.mStatement.executeQuery(this.mQuery);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public String getString(int columnIndex) {
        try {
            return resultSet != null ? resultSet.getString(columnIndex + 1) : null;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    @Override
    public boolean moveToNext() {
        try {
            return resultSet != null && resultSet.next();
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
            // System.out.println("count:" + count + " q:" + mQuery);
        } catch (Exception e) {
            System.out.println("count-error: " + e.getMessage());
        }
        return count;
    }

    @Override
    public void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            mStatement.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

