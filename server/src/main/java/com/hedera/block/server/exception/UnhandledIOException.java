package com.hedera.block.server.exception;

import java.io.Serial;

/**
 * UnhandledIOException is a RuntimeException that wraps an IOException.
 * This can be used to rethrow checked IOExceptions as unchecked exceptions.
 */
public class UnhandledIOException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new UnhandledIOException with the specified cause.
     *
     * @param cause the underlying IOException that caused this exception
     */
    public UnhandledIOException(final Throwable cause) {
        super(cause);
    }
}
