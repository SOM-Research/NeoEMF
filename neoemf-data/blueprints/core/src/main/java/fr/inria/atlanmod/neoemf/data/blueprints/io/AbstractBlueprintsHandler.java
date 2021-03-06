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

package fr.inria.atlanmod.neoemf.data.blueprints.io;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.core.StringId;
import fr.inria.atlanmod.neoemf.data.blueprints.BlueprintsPersistenceBackend;
import fr.inria.atlanmod.neoemf.data.store.PersistentStore;
import fr.inria.atlanmod.neoemf.io.AlreadyExistingIdException;
import fr.inria.atlanmod.neoemf.io.Handler;
import fr.inria.atlanmod.neoemf.io.persistence.AbstractPersistenceHandler;
import fr.inria.atlanmod.neoemf.io.structure.Element;
import fr.inria.atlanmod.neoemf.io.structure.Identifier;
import fr.inria.atlanmod.neoemf.io.structure.MetaClass;

import java.util.NoSuchElementException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * An abstract {@link Handler} specific to a Blueprints implementation.
 */
public abstract class AbstractBlueprintsHandler extends AbstractPersistenceHandler<BlueprintsPersistenceBackend> {

    /**
     * The string used as a separator between values of multi-valued attributes.
     *
     * @see fr.inria.atlanmod.neoemf.data.blueprints.store.DirectWriteBlueprintsStore#SEPARATOR
     */
    private static final char SEPARATOR = ':';

    /**
     * The property key used to define the index of an edge.
     */
    private static final String POSITION = "position";

    /**
     * The label used to link root vertex to top-level elements.
     */
    private static final String CONTAINER = "eContainer";

    /**
     * The property key used to define the opposite containing feature in container {@link Edge}s.
     */
    private static final String CONTAINING_FEATURE = "containingFeature";

    /**
     * The property key used to define the number of edges with a specific label.
     */
    private static final String SIZE_LITERAL = "size";

    /**
     * The identifier of the root element.
     */
    private static final Id ROOT_ID = new StringId("ROOT");

    /**
     * The property key used by the root element to define its content.
     */
    private static final String ROOT_FEATURE_NAME = "eContents";

    /**
     * In-memory cache that holds recently processed {@link Vertex}s, identified by the associated object
     * {@link Id}.
     */
    protected final Cache<Id, Vertex> verticesCache;

    /**
     * Constructs a new {@code AbstractBlueprintsHandler} on the given {@code backend}.
     *
     * @param backend the back-end where to store data
     */
    public AbstractBlueprintsHandler(BlueprintsPersistenceBackend backend) {
        super(backend);
        this.verticesCache = Caffeine.newBuilder().maximumSize(DEFAULT_CACHE_SIZE).build();
    }

    /**
     * Updates the containment identified by its {@code name} between the {@code parentVertex} and the {@code
     * childVertex}.
     *
     * @param name    the name of the property identifying the reference (parent -&gt; child)
     * @param parentVertex the parent vertex
     * @param childVertex  the child vertex
     */
    private static void updateContainment(final String name, final Vertex parentVertex, final Vertex childVertex) {
        for (Edge edge : childVertex.getEdges(Direction.OUT, CONTAINER)) {
            edge.remove();
        }

        Edge edge = childVertex.addEdge(CONTAINER, parentVertex);
        edge.setProperty(CONTAINING_FEATURE, name);
    }

    /**
     * Returns the size of the property identified by its {@code name} in the given {@code vertex}.
     *
     * @param vertex the vertex containing the property
     * @param name   the name of the property
     *
     * @return the size
     */
    private static Integer getSize(final Vertex vertex, final String name) {
        Integer size = vertex.getProperty(formatKeyValue(name, SIZE_LITERAL));
        return isNull(size) ? 0 : size;
    }

    /**
     * Defines the {@code size} of the property identified by its {@code name} in the given {@code vertex}.
     *
     * @param vertex the vertex containing the property
     * @param name   the name of the property
     * @param size   the size
     */
    private static void setSize(final Vertex vertex, final String name, final int size) {
        vertex.setProperty(formatKeyValue(name, SIZE_LITERAL), size);
    }

    /**
     * Formats a key/value pair as {@code "key:value"}.
     *
     * @param key   the key
     * @param value the value of the {@code key}
     *
     * @return the literal representation of a key/value pair
     */
    private static String formatKeyValue(final String key, final Object value) {
        return key + SEPARATOR + value;
    }

    @Override
    protected Id getId(final String reference) {
        return new StringId(reference);
    }

    @Override
    protected void addElement(final Id id, final String nsUri, final String name, final boolean root) {
        Vertex vertex = createVertex(id);

        // Checks if the Vertex is not already defined
        if (nonNull(vertex.getProperty(BlueprintsPersistenceBackend.KEY_EPACKAGE_NSURI))) {
            throw new IllegalArgumentException(
                    "An element with the same Id (" + id.toString() + ") is already defined. " +
                            "Use a handler with a conflicts resolution feature instead.");
        }

        if (nonNull(name)) {
            vertex.setProperty(BlueprintsPersistenceBackend.KEY_ECLASS_NAME, name);
        }
        vertex.setProperty(BlueprintsPersistenceBackend.KEY_EPACKAGE_NSURI, nsUri);

        if (root) {
            // Add the current element as content of the 'ROOT' node
            addReference(ROOT_ID, ROOT_FEATURE_NAME, PersistentStore.NO_INDEX, false, false, id);
        }
    }

    @Override
    protected void addAttribute(final Id id, final String name, int index, final boolean many, final Object value) {
        Vertex vertex = getVertex(id);

        int size = getSize(vertex, name);

        if (index == PersistentStore.NO_INDEX) {
            index = size;
        }

        size++;
        setSize(vertex, name, size);

        vertex.setProperty(many ? formatKeyValue(name, index) : name, value);
    }

    @Override
    protected void addReference(final Id id, final String name, int index, final boolean many, final boolean containment, final Id idReference) {
        Vertex vertex = getVertex(id);
        Vertex referencedVertex = getVertex(idReference);

        // Update the containment reference if needed
        if (containment) {
            updateContainment(name, vertex, referencedVertex);
        }

        int size = getSize(vertex, name);

        if (index == PersistentStore.NO_INDEX) {
            index = size;
        }

        size++;
        setSize(vertex, name, size);

        Edge edge = vertex.addEdge(name, referencedVertex);
        edge.setProperty(POSITION, index);
    }

    @Override
    protected void setMetaClass(final Id id, final Id metaClassId) {
        Vertex vertex = getVertex(id);
        Vertex metaClassVertex = getVertex(metaClassId);

        vertex.addEdge(BlueprintsPersistenceBackend.KEY_INSTANCE_OF, metaClassVertex);
    }

    @Override
    public void handleStartDocument() {
        createRootVertex();

        super.handleStartDocument();
    }

    /**
     * Returns the {@link Vertex} which has the specified {@code id}.
     *
     * @param id the identifier of the vertex
     *
     * @return the {@link Vertex}
     *
     * @throws NoSuchElementException if no element is found with the {@code id}
     */
    protected abstract Vertex getVertex(final Id id);

    /**
     * Creates a {@link Vertex} identified by the specified {@code id}.
     *
     * @param id the identifier of the vertex
     *
     * @return the new {@link Vertex}
     *
     * @throws AlreadyExistingIdException if the {@code id} is already used as primary key for another element
     */
    protected abstract Vertex createVertex(final Id id);

    /**
     * Creates the root element.
     */
    private void createRootVertex() {
        // Create the 'ROOT' node with the default metaclass
        MetaClass metaClass = MetaClass.getDefault();

        Element rootElement = new Element(metaClass.ns(), metaClass.name());

        rootElement.id(Identifier.generated(ROOT_ID.toString()));
        rootElement.className(metaClass.name());
        rootElement.root(false);
        rootElement.metaClass(metaClass);

        Id id = createElement(rootElement, ROOT_ID);
        Id metaClassId = getOrCreateMetaClass(metaClass);

        setMetaClass(id, metaClassId);
    }
}
