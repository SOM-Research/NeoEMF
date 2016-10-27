/*
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 */
package fr.inria.atlanmod.neoemf.map.datastore.estores.impl;

import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.core.impl.StringId;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.IOException;

/**
 * Serializer for persistent object ids. Only works with StringId instances.
 *
 */
public class IdSerializer implements Serializer<Id> {

    Serializer<String> serializer = Serializer.STRING;

    /**
     * Serializes id to a DataOutput. Uses a String Serializer.
     * @param out
     * @param id
     * @throws IOException
     */
    @Override
    public void serialize(DataOutput2 out, Id id) throws IOException {
        serializer.serialize(out, id.toString());
    }

    @Override
    public Id deserialize(DataInput2 in, int i) throws IOException {
        return new StringId(serializer.deserialize(in, i));

    }

    @Override
    public boolean equals(Object other) {

        if (other == null || !(other instanceof IdSerializer))return false;
        if (this == other) return true;

        IdSerializer that = (IdSerializer) other;

        return serializer.equals(that.serializer);
    }

    public int compare(Id one, Id other) {
        return one.compareTo(other);
    }

}