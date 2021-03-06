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

package fr.inria.atlanmod.neoemf.io.hash;

/**
 * A hashcode with a {@link String} representation.
 */
class StringHashCode implements HashCode {

    /**
     * The literal representation of this hashcode.
     */
    private final String hashCode;

    /**
     * Constructs a new {@code StringHashCode} from the {@code hashCode}.
     *
     * @param hashCode the literal representation of this hashcode
     */
    public StringHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Override
    public String toString() {
        return hashCode;
    }
}
