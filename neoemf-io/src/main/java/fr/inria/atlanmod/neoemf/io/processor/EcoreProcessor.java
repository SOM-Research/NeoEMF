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

package fr.inria.atlanmod.neoemf.io.processor;

import fr.inria.atlanmod.neoemf.io.Handler;
import fr.inria.atlanmod.neoemf.io.structure.Attribute;
import fr.inria.atlanmod.neoemf.io.structure.Element;
import fr.inria.atlanmod.neoemf.io.structure.Identifier;
import fr.inria.atlanmod.neoemf.io.structure.MetaClass;
import fr.inria.atlanmod.neoemf.io.structure.Namespace;
import fr.inria.atlanmod.neoemf.io.structure.Reference;
import fr.inria.atlanmod.neoemf.util.logging.NeoLogger;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A {@link Processor} that creates and links simple elements to an EMF structure.
 */
public class EcoreProcessor extends AbstractProcessor {

    /**
     * Stack containing previous {@link EClass}.
     */
    private final Deque<EClass> classesStack;

    /**
     * Stack containing previous identifier.
     */
    private final Deque<Identifier> idsStack;

    /**
     * Attribute waiting a value (via {@link #handleCharacters(String)}.
     */
    private Attribute waitingAttribute;

    /**
     * Defines if the previous element was an attribute, or not.
     */
    private boolean previousWasAttribute;

    /**
     * Constructs a new {@code EcoreProcessor} with the given {@code handler}.
     *
     * @param handler the handler to notify
     */
    public EcoreProcessor(Handler handler) {
        super(handler);
        this.classesStack = new ArrayDeque<>();
        this.idsStack = new ArrayDeque<>();
        this.previousWasAttribute = false;
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod") // Redirect
    public void handleStartElement(Element element) {
        // Is root
        if (classesStack.isEmpty()) {
            processRootElement(element);
        }
        // Is a feature of parent
        else {
            processFeature(element);
        }
    }

    @Override
    public void handleAttribute(Attribute attribute) {
        EClass eClass = classesStack.getLast();
        EStructuralFeature feature = eClass.getEStructuralFeature(attribute.name());

        // Checks that the attribute is well a attribute
        if (feature instanceof EAttribute) {
            EAttribute eAttribute = (EAttribute) feature;
            attribute.many(eAttribute.isMany());
            super.handleAttribute(attribute);
        }

        // Otherwise redirect to the reference handler
        else if (feature instanceof EReference) {
            handleReference(Reference.from(attribute));
        }
    }

    @Override
    public void handleReference(Reference reference) {
        EClass eClass = classesStack.getLast();
        EStructuralFeature feature = eClass.getEStructuralFeature(reference.name());

        // Checks that the reference is well a reference
        if (feature instanceof EReference) {
            EReference eReference = (EReference) feature;
            reference.containment(eReference.isContainment());
            reference.many(eReference.isMany());
            super.handleReference(reference);
        }

        // Otherwise redirect to the attribute handler
        else if (feature instanceof EAttribute) {
            handleAttribute(Attribute.from(reference));
        }
    }

    @Override
    public void handleEndElement() {
        if (!previousWasAttribute) {
            classesStack.removeLast();
            idsStack.removeLast();

            super.handleEndElement();
        }
        else {
            NeoLogger.warn("An attribute still waiting for a value : it will be ignored");
            waitingAttribute = null; // Clean the waiting attribute : no character has been found to fill its value
            previousWasAttribute = false;
        }
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod") // Redirect
    public void handleCharacters(String characters) {
        // Defines the value of the waiting attribute, if exists
        if (nonNull(waitingAttribute)) {
            waitingAttribute.value(characters);
            handleAttribute(waitingAttribute);

            waitingAttribute = null;
        }
    }

    /**
     * Creates the root element from the given {@code element}.
     *
     * @param element the element representing the root element
     *
     * @throws NullPointerException if the {@code element} does not have a namespace
     */
    private void processRootElement(Element element) {
        Namespace ns = checkNotNull(element.ns(), "The root element must have a namespace");

        // Retrieves the EPackage from NS prefix
        EPackage ePackage = checkNotNull((EPackage) EPackage.Registry.INSTANCE.get(ns.uri()),
                "EPackage %s is not registered.", ns.uri());

        // Gets the current EClass
        EClass eClass = (EClass) ePackage.getEClassifier(element.name());

        // Defines the metaclass of the current element if not present
        if (isNull(element.metaClass())) {
            element.metaClass(new MetaClass(ns, eClass.getName()));
        }

        // Defines the element as root node
        element.root(true);

        // Notifies next handlers
        super.handleStartElement(element);

        // Saves the current EClass
        classesStack.addLast(eClass);

        // Gets the identifier of the element created by next handlers, and save it
        idsStack.addLast(element.id());
    }

    /**
     * Processes a feature and redirects the processing to the associated method according to its type (attribute or
     * reference).
     *
     * @param element the element representing the feature
     *
     * @see #processAttribute(Element, EAttribute)
     * @see #processReference(Element, Namespace, EReference, EPackage)
     */
    private void processFeature(Element element) {
        // Retrieve the parent EClass
        EClass parentEClass = classesStack.getLast();

        // Gets the EPackage from it
        EPackage ePackage = parentEClass.getEPackage();
        Namespace ns = Namespace.Registry.getInstance().getFromPrefix(ePackage.getNsPrefix());

        // Gets the structural feature from the parent, according the its local name (the attr/ref name)
        EStructuralFeature feature = parentEClass.getEStructuralFeature(element.name());

        if (feature instanceof EAttribute) {
            processAttribute(element, (EAttribute) feature);
        }
        else if (feature instanceof EReference) {
            processReference(element, ns, (EReference) feature, ePackage);
        }
    }

    /**
     * Processes an attribute.
     *
     * @param element the element representing the attribute
     * @param attribute  the associated EMF attribute
     */
    private void processAttribute(@SuppressWarnings("unused") Element element, EAttribute attribute) {
        if (nonNull(waitingAttribute)) {
            NeoLogger.warn("An attribute still waiting for a value : it will be ignored");
        }

        // Waiting a plain text value
        waitingAttribute = new Attribute(attribute.getName());
        previousWasAttribute = true;
    }

    /**
     * Processes a reference.
     *
     * @param element the element representing the reference
     * @param ns         the namespace of the class of the reference
     * @param reference  the associated EMF reference
     * @param ePackage   the package where to find the class of the reference
     */
    private void processReference(Element element, Namespace ns, EReference reference, EPackage ePackage) {
        // Gets the type the reference or gets the type from the registered metaclass
        EClass eClass = getEClass(element, ns, (EClass) reference.getEType(), ePackage);

        element.ns(ns);

        // Notify next handlers of new element, and retrieve its identifier
        super.handleStartElement(element);
        Identifier currentId = element.id();

        // Create a reference from the parent to this element, with the given local name
        if (reference.isContainment()) {
            NeoLogger.debug("{0}#{1} is a containment : creating the reverse reference.", element.metaClass(), reference.getName());

            Reference ref = new Reference(reference.getName());
            ref.id(idsStack.getLast());
            ref.idReference(currentId);

            handleReference(ref);
        }

        // Save EClass and identifier
        classesStack.addLast(eClass);
        idsStack.addLast(currentId);
    }

    /**
     * Returns the {@link EClass} associated with the given {@code element}.
     *
     * @param element    the element representing the class
     * @param ns         the namespace of the {@code superClass}
     * @param superClass the super-type of the sought class
     * @param ePackage   the package where to find the class
     *
     * @return a class
     *
     * @throws IllegalArgumentException if the {@code superClass} is not the super-type of the sought class
     */
    @Nonnull
    private EClass getEClass(Element element, Namespace ns, EClass superClass, EPackage ePackage) {
        MetaClass metaClass = element.metaClass();

        if (nonNull(metaClass)) {
            EClass subEClass = (EClass) ePackage.getEClassifier(metaClass.name());

            // Checks that the metaclass is a subtype of the reference type.
            // If true, use it instead of supertype
            if (superClass.isSuperTypeOf(subEClass)) {
                superClass = subEClass;
            }
            else {
                throw new IllegalArgumentException(subEClass.getName() + " is not a subclass of " + superClass.getName());
            }
        }

        // If not present, create the metaclass from the current class
        else {
            metaClass = new MetaClass(ns, superClass.getName());
            element.metaClass(metaClass);
        }

        return superClass;
    }
}
