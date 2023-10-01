/**
 * MIT License
 * <p>
 * Copyright (c) [2023] [Shahriar Zaman]
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package media.uqab.fuzzybleJava;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A simple implementation of {@link java.util.function} interfaces
 * to support android versions before api 24.
 * <p>
 * author: github/fCat97
 * date: 01/10/2023
 */
public class MyStream<T> {
    private final Collection<T> items;

    private MyStream(Collection<T> items) {
        this.items = items;
    }

    /**
     * Create a stream object
     * @param items items of stream
     * @return Stream object
     */
    public static <T> MyStream<T> of(List<T> items) {
        return new MyStream<>(items);
    }

    public static <T> MyStream<T> of(Collection<T> items) {
        return new MyStream<>(items);
    }

    /**
     * Convert the stream to list
     * @return list of items
     */
    public List<T> toList() {
        return new ArrayList<>(items);
    }

    public MyStream<T> filter(Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T t: items) {
            if (predicate.test(t)) {
                result.add(t);
            }
        }

        return new MyStream<>(result);
    }

    public <R> MyStream<R> map(Function<T, R> function) {
        List<R> result = new ArrayList<>();
        for (T t: items) {
            R r = function.apply(t);
            result.add(r);
        }

        return new MyStream<>(result);
    }

    public  <R> MyStream<R> flatMap(Function<T, R[]> function) {
        List<R> result = new ArrayList<>();
        for (T t: items) {
            R[] list = function.apply(t);
            Collections.addAll(result, list);
        }

        return new MyStream<>(result);
    }

    public void forEach(Consumer<T> consumer) {
        for (T t: items) {consumer.consume(t);}
    }

    public interface Predicate<T> { boolean test(T t); }
    public interface Function<T, R> { R apply(T t); }

    public interface Consumer<T> { void consume(T t); }
}