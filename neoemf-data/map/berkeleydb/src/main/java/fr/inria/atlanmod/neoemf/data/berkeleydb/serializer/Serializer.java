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

package fr.inria.atlanmod.neoemf.data.berkeleydb.serializer;

import fr.inria.atlanmod.neoemf.annotations.Experimental;

/**
 * ???
 *
 * @param <T> ???
 */
@Experimental
public interface Serializer<T> {

    /**
     * ???
     *
     * @param value ???
     *
     * @return ???
     */
    byte[] serialize(T value);

    /**
     * ???
     *
     * @param data ???
     *
     * @return ???
     */
    T deserialize(byte[] data);
}
