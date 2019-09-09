package org.envirocar.app.rxutils;

/**
 * @param <M> the optional parameter type
 * @author dewall
 */
public class Optional<M> {
    private final M optional;

    public static <T> Optional<T> create(T t){
        return new Optional<>(t);
    }

    /**
     * Constructor.
     *
     * @param optional
     */
    public Optional(M optional) {
        this.optional = optional;
    }

    public M getOptional() {
        return optional;
    }

    public boolean isEmpty() {
        return this.optional == null;
    }
}
