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

package fr.inria.atlanmod.neoemf.io.writer;

import fr.inria.atlanmod.neoemf.annotations.Experimental;
import fr.inria.atlanmod.neoemf.io.structure.Attribute;
import fr.inria.atlanmod.neoemf.io.structure.Element;
import fr.inria.atlanmod.neoemf.io.structure.Reference;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 */
@Experimental
public abstract class AbstractXmiWriter extends AbstractWriter {

    @Override
    public void write(OutputStream stream) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void handleStartDocument() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void handleStartElement(Element element) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void handleAttribute(Attribute attribute) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void handleReference(Reference reference) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void handleEndElement() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void handleEndDocument() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void handleCharacters(String characters) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
