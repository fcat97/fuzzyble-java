package com.github.fCat97.database;

import media.uqab.fuzzybleJava.Fuzzyble;
import media.uqab.fuzzybleJava.SqlCursor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Database implements Fuzzyble {
    private static final HashMap<String, Database> dbPool = new HashMap<>();
    private final Connection mConnection;

    Database(Path dbPath, boolean createIfNotExist) throws SQLException, ClassNotFoundException {
        if (!createIfNotExist && !Files.exists(dbPath)) {
            throw new SQLException("file not exist");
        }

        Class.forName("org.sqlite.JDBC");
        mConnection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }

    public static synchronized Database getInstance(Path db) throws SQLException, ClassNotFoundException {
        return getInstance(db, false);
    }

    public static synchronized Database getInstance(Path db, boolean createIfNotExist) throws SQLException, ClassNotFoundException {
        var database = dbPool.getOrDefault(db.toString(), null);
        if (database == null) {
            database = new Database(db, createIfNotExist);
            dbPool.put(db.toString(), database);
        }

        return database;
    }

    @Override
    public SqlCursor onQuery(String query) {
        return new QueryCursor(mConnection, query);
    }

    @Override
    public SqlCursor onQuery(String query, String[] args) {
        return new QueryCursor(mConnection, prepareQuery(query, args));
    }

    @Override
    public void onExecute(String sql, String[] args) {
        try {
            final var stmt = mConnection.createStatement();
            if (args == null) {
                stmt.executeUpdate(sql);
            } else {
                stmt.executeUpdate(prepareQuery(sql, args));
            }
        } catch (SQLException e) {
            System.out.println("failed to execute: " + prepareQuery(sql, args));
        }
    }

    public List<String> getTables() {
        final var cursor = query("SELECT * FROM sqlite_master where type='table';");
        if (cursor == null) {
            return new ArrayList<>();
        }

        final var result = new ArrayList<String>();
        try {
            while (cursor.next()) {
                result.add(cursor.getString(2));
            }
            cursor.close();
            cursor.getStatement().close();
        } catch (SQLException e) {
            System.out.println("failed to get tables");
        }

        return result;
    }

    public List<String> getColumns(String tableName) {
        final var result = new ArrayList<String>();
        final var rs = query("select * from " + tableName + " LIMIT 0");
        if (rs == null) return new ArrayList<>();

        try {
            final var mrs = rs.getMetaData();
            for (int i = 1; i <= mrs.getColumnCount(); i++) {
                final var row = new Object[3];
                row[0] = mrs.getColumnLabel(i);
                row[1] = mrs.getColumnTypeName(i);
                row[2] = mrs.getPrecision(i);

                result.add(row[0].toString());
            }
            rs.close();
            rs.getStatement().close();
        } catch (SQLException e) {
            System.out.println("failed to get columns");
        }

        return result;
    }

    public List<String> getFirst10Row(String tableName) {
        final var result = new ArrayList<String>();
        final var columns = getColumns(tableName);
        final var rs = query("select * from " + tableName + " LIMIT 10");
        if (rs == null) return result;

        try {
            while (rs.next()) {
                final var row = new ArrayList<String>();
                for (int i = 1; i <= columns.size(); i++) {
                    var v = switch (rs.getMetaData().getColumnTypeName(i)) {
                        case "INTEGER" -> String.valueOf(rs.getInt(i));
                        case "TEXT" -> rs.getString(i);
                        default -> rs.getString(i);
                    };

                    row.add(Objects.requireNonNullElse(v, "null"));
                }
                var joined = String.join(SEPARATOR, row);
                result.add(joined);
            }

            rs.close();
            rs.getStatement().close();
        } catch (SQLException e) {
            System.out.println("failed to getFirst100Row " + e.getMessage());
        }

        return result;
    }

    public List<String> getData(String tableName, String columnName) {
        final var result = new ArrayList<String>();
        final var rs = query("select " + columnName + " from "+ tableName);
        if (rs == null) return result;

        try {
            while (rs.next()) {
                var s = rs.getString(columnName);
                result.add(s);
            }
            rs.close();
            rs.getStatement().close();
        } catch (SQLException e) {
            System.out.println("failed to getData " + e.getMessage());
        }

        return result;
    }

    public List<String> getData(String tableName, String columnName, int limit) {
        final var result = new ArrayList<String>();
        final var rs = query("select " + columnName + " from "+ tableName + " limit " + limit);
        if (rs == null) return result;

        try {
            while (rs.next()) {
                var s = rs.getString(columnName);
                result.add(s);
            }
            rs.close();
            rs.getStatement().close();
        } catch (SQLException e) {
            System.out.println("failed to getData " + e.getMessage());
        }

        return result;
    }

    public List<String> searchItems(String tableName, String columnName, String search) {
        final var result = new ArrayList<String>();
        final var columns = getColumns(tableName);
        final var rs = query("select * from $tableName where $columnName LIKE '%$search%' LIMIT 100");
        if (rs == null) return result;

        var c = 0;
        try {
            while (rs.next()) {
                final var row = new ArrayList<String>();
                for (int i = 1; i <= columns.size(); i++) {
                    var v = switch (rs.getMetaData().getColumnTypeName(i)) {
                        case "INTEGER" -> String.valueOf(rs.getInt(i));
                        case "TEXT" -> rs.getString(i);
                        default -> rs.getString(i);
                    };

                    row.add(Objects.requireNonNullElse(v, "null"));
                }
                c++;
                result.add(String.join(SEPARATOR, row));
                System.out.println("found " + c + " items for $search");
                rs.close();
                rs.getStatement().close();
            }
        } catch (SQLException e) {
            System.out.println("failed to search " + e.getMessage());
        }
        return result;
    }

    private ResultSet query(String sql) {
        try {
            final Statement stmt = mConnection.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            System.out.println("failed to execute: " + sql);
            return null;
        }
    }

    private String prepareQuery(String query, String[] args) {
        if (args == null) return query;
        var mutQuery = query;
        for (String a : args) {
            mutQuery = mutQuery.replaceFirst("\\?", "'" + a + "'");
        }
        return mutQuery;
    }

    public void close() {
        try {
            if(!mConnection.isClosed()) {
                mConnection.close();
            }
        } catch (SQLException e) {
            System.out.println("failed to close db " + e.getMessage());
        }
    }

    private static final String SEPARATOR = "-:-";
}
