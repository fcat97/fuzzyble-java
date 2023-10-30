package media.uqab.fuzzybleJava.impl;

import java.sql.Connection;

public class ImmutableDatabase extends MockDatabase {

        public ImmutableDatabase(Connection connection) {
            super(connection);
        }

        @Override
        public void onExecute(String sql, String[] args) {
            throw new RuntimeException("can't execute on immutable database");
        }
    }