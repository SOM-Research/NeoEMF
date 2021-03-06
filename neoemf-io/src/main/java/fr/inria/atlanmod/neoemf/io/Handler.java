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

package fr.inria.atlanmod.neoemf.io;

import fr.inria.atlanmod.neoemf.io.structure.Attribute;
import fr.inria.atlanmod.neoemf.io.structure.Element;
import fr.inria.atlanmod.neoemf.io.structure.Reference;

/**
 * A object that handles events notified by a {@link Notifier}.
 *
 * @see Notifier
 */
public interface Handler {

    /**
     * Process the start of a document.
     *
     * @see Notifier#notifyStartDocument()
     */
    void handleStartDocument();

    /**
     * Process the start of an element.
     *
     * @param element the element of the new element
     *
     * @see Notifier#notifyStartElement(Element)
     */
    void handleStartElement(Element element);

    /**
     * Process an attribute in the current element.
     * <p>
     * An attribute is a simple key/value.
     *
     * @param attribute the new attribute
     *
     * @see Notifier#notifyAttribute(Attribute)
     */
    void handleAttribute(Attribute attribute);

    /**
     * Process a reference from the current element to another element.
     * <p>
     * A reference is an attribute which is link to another element.
     *
     * @param reference the new reference
     *
     * @see Notifier#notifyReference(Reference)
     */
    void handleReference(Reference reference);

    /**
     * Process the end of the current element.
     *
     * @see Notifier#notifyEndElement()
     */
    void handleEndElement();

    /**
     * Process the end of a document.
     *
     * @see Notifier#notifyEndDocument()
     */
    void handleEndDocument();

    /**
     * Process a set of characters.
     *
     * @param characters the new characters
     *
     * @see Notifier#notifyCharacters(String)
     */
    void handleCharacters(String characters);
}
