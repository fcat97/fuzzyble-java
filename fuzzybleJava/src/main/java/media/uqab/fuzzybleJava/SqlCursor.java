package media.uqab.fuzzybleJava;

import java.io.Closeable;

public interface SqlCursor extends Closeable {

    String getString(int columnIndex);

    boolean moveToNext();

    int count();
}
