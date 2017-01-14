/*
 * Copyright (c) 2013-2017 Atlanmod INRIA LINA Mines Nantes.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 */

package fr.inria.atlanmod.neoemf.data;

import java.io.IOException;

/**
 * Exception thrown when an error occurred when creating {@link PersistenceBackend} or
 * {@link fr.inria.atlanmod.neoemf.data.store.PersistentStore} in {@link PersistenceBackendFactory}.
 */
public class InvalidDataStoreException extends IOException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an {@code InvalidDataStoreException} with {@code null} as its error detail message.
     */
    public InvalidDataStoreException() {
    }

    /**
     * Constructs an {@code InvalidDataStoreException} with the specified detail {@code message}.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     */
    public InvalidDataStoreException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code InvalidDataStoreException} with the specified {@code cause} and a detail message of
     * {@code (cause==null ? null : cause.toString())} (which typically contains the class and detail message of cause).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null}
     *              value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public InvalidDataStoreException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an {@code InvalidDataStoreException} with the specified detail {@code message} and {@code cause}.
     *
     * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method)
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null}
     *                value is permitted, and indicates that the cause is nonexistent or unknown.)
     *
     * @note The detail message associated with cause is not automatically incorporated into this exception's detail
     * message.
     */
    public InvalidDataStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}

