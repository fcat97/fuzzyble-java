package media.uqab.fuzzybleJava.impl;

import media.uqab.fuzzybleJava.Fuzzyble;
import media.uqab.fuzzybleJava.SqlCursor;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;

import static media.uqab.fuzzybleJava.impl.Log.log;

public class MockDatabase implements Fuzzyble {
    private final Connection connection;

    public MockDatabase(Connection connection) {
        this.connection = connection;
    }

    @Override
    public SqlCursor onQuery(String query) {
        try {
            log(query);
            return new QueryCursor(connection, query);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public SqlCursor onQuery(String query, String[] args) {
        try {
            String pQuery = prepareQuery(query, args);
            log(pQuery);
            return new QueryCursor(connection, pQuery);
        } catch (Exception e) {
            log(e.getMessage());
            return null;
        }
    }

    @Override
    public void onExecute(String sql, String[] args) {
        try {
            String command = prepareQuery(sql, args);
            log(command);

            Statement statement = connection.createStatement();
            statement.executeUpdate(command);
            statement.close();
        } catch (Exception e) {
            log(e.getMessage());
        }
    }

    private String prepareQuery(String query, String[] args) {
        if (args == null) return query;

        String mutQuery = query;
        for (String a : args) {
            mutQuery = mutQuery.replaceFirst("\\?", "'" + a + "'");
        }
        return mutQuery;
    }
}