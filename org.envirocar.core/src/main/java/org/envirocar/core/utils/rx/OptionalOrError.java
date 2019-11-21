package org.envirocar.core.utils.rx;

public class OptionalOrError<T> extends Optional<T> {

    public static <T> OptionalOrError<T> create(T t) {
        return new OptionalOrError<T>(t);
    }

    public static <T> OptionalOrError<T> create(Exception e) {
        return new OptionalOrError<T>(e);
    }

    private final Exception e;

    /**
     * Constructor.
     *
     * @param optional
     */
    public OptionalOrError(T optional) {
        this(optional, null);
    }

    public OptionalOrError(Exception e) {
        this(null, e);
    }

    public OptionalOrError(T optional, Exception e) {
        super(optional);
        this.e = e;
    }

    public Exception getE() {
        return e;
    }

    public boolean isSuccessful(){
        return e == null;
    }
}
