package com.github.davidmoten.rx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.davidmoten.rx.internal.operators.OnSubscribeReader;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;

public final class Strings {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    /**
     * Returns null if input is null otherwise returns input.toString().trim().
     */
    private static Func1<Object, String> TRIM = new Func1<Object, String>() {

        @Override
        public String call(Object input) {
            if (input == null)
                return null;
            else
                return input.toString().trim();
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> Func1<T, String> trim() {
        return (Func1<T, String>) TRIM;
    }

    public static Observable<String> from(Reader reader, int bufferSize) {
        return Observable.create(new OnSubscribeReader(reader, bufferSize));
    }

    public static Observable<String> from(Reader reader) {
        return from(reader, 8192);
    }

    public static Observable<String> from(InputStream is) {
        return from(new InputStreamReader(is));
    }

    public static Observable<String> from(InputStream is, Charset charset) {
        return from(new InputStreamReader(is, charset));
    }

    public static Observable<String> from(InputStream is, Charset charset, int bufferSize) {
        return from(new InputStreamReader(is, charset), bufferSize);
    }

    public static Observable<String> split(Observable<String> source, String pattern) {
        return source.compose(Transformers.split(pattern));
    }

    public static Observable<String> concat(Observable<String> source) {
        return join(source, "");
    }

    public static Observable<String> concat(Observable<String> source, final String delimiter) {
        return join(source, delimiter);
    }

    public static Observable<String> strings(Observable<?> source) {
        return source.map(new Func1<Object, String>() {
            @Override
            public String call(Object t) {
                return String.valueOf(t);
            }
        });
    }

    public static Observable<String> from(File file) {
        return from(file, DEFAULT_CHARSET);
    }

    public static Observable<String> from(final File file, final Charset charset) {
        Func0<Reader> resourceFactory = new Func0<Reader>() {
            @Override
            public Reader call() {
                try {
                    return new InputStreamReader(new FileInputStream(file), charset);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Func1<Reader, Observable<String>> observableFactory = new Func1<Reader, Observable<String>>() {
            @Override
            public Observable<String> call(Reader is) {
                return from(is);
            }
        };
        Action1<Reader> disposeAction = new Action1<Reader>() {
            @Override
            public void call(Reader is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        return Observable.using(resourceFactory, observableFactory, disposeAction, true);
    }

    public static Observable<String> join(Observable<String> source) {
        return join(source, "");
    }

    public static Observable<String> join(final Observable<String> source, final String delimiter) {

        return Observable.defer(new Func0<Observable<String>>() {
            final AtomicBoolean afterFirst = new AtomicBoolean(false);
            final AtomicBoolean isEmpty = new AtomicBoolean(true);

            @Override
            public Observable<String> call() {
                return source.collect(new Func0<StringBuilder>() {
                    @Override
                    public StringBuilder call() {
                        return new StringBuilder();
                    }
                }, new Action2<StringBuilder, String>() {

                    @Override
                    public void call(StringBuilder b, String s) {
                        if (!afterFirst.compareAndSet(false, true)) {
                            b.append(delimiter);
                        }
                        b.append(s);
                        isEmpty.set(false);
                    }
                }).flatMap(new Func1<StringBuilder, Observable<String>>() {

                    @Override
                    public Observable<String> call(StringBuilder b) {
                        if (isEmpty.get())
                            return Observable.empty();
                        else
                            return Observable.just(b.toString());
                    }
                });

            }
        });
    }

}