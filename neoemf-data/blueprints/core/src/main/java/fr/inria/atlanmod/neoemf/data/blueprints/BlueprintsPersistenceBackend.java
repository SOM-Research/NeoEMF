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

package fr.inria.atlanmod.neoemf.data.blueprints;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.GraphHelper;
import com.tinkerpop.blueprints.util.wrappers.id.IdEdge;
import com.tinkerpop.blueprints.util.wrappers.id.IdGraph;

import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.core.PersistenceFactory;
import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import fr.inria.atlanmod.neoemf.core.StringId;
import fr.inria.atlanmod.neoemf.data.AbstractPersistenceBackend;
import fr.inria.atlanmod.neoemf.data.blueprints.store.DirectWriteBlueprintsCacheManyStore;
import fr.inria.atlanmod.neoemf.data.blueprints.store.DirectWriteBlueprintsStore;
import fr.inria.atlanmod.neoemf.data.structure.ClassInfo;
import fr.inria.atlanmod.neoemf.logging.NeoLogger;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EPackageImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A {@link fr.inria.atlanmod.neoemf.data.PersistenceBackend} that is responsible of low-level access to a Blueprints
 * database.
 * <p>
 * It wraps an existing Blueprints database and provides facilities to create and retrieve elements, map {@link
 * PersistentEObject}s to {@link Vertex} elements in order to speed up attribute access, and manage a set of lightweight
 * caches to improve access time of {@link Vertex} from  their corresponding {@link PersistentEObject}.
 *
 * @note This class is used in {@link DirectWriteBlueprintsStore} and {@link DirectWriteBlueprintsCacheManyStore} to
 * access and manipulate the database.
 * @note Instances of {@link BlueprintsPersistenceBackend} are created by {@link BlueprintsPersistenceBackendFactory}
 * that provides an usable {@link KeyIndexableGraph} that can be manipulated by this wrapper.
 * @see BlueprintsPersistenceBackendFactory
 * @see DirectWriteBlueprintsStore
 * @see DirectWriteBlueprintsCacheManyStore
 */
public class BlueprintsPersistenceBackend extends AbstractPersistenceBackend {

    /**
     * The literal description of this back-end.
     */
    public static final String NAME = "blueprints";
    /**
     * The property key used to set metaclass name in metaclass vertices
     */
    public static final String KEY_ECLASS_NAME = EcorePackage.eINSTANCE.getENamedElement_Name().getName();
    /**
     * The property key used to set the {@link EPackage} {@code nsURI} in metaclass vertices
     */
    public static final String KEY_EPACKAGE_NSURI = EcorePackage.eINSTANCE.getEPackage_NsURI().getName();
    /**
     * The label of type conformance {@link Edge}s
     */
    public static final String KEY_INSTANCE_OF = "kyanosInstanceOf";
    /**
     * The name of the index entry holding metaclass vertices
     */
    public static final String KEY_METACLASSES = "metaclasses";
    /**
     * The index key used to retrieve metaclass vertices
     */
    public static final String KEY_NAME = "name";

    // TODO Find the more predictable maximum cache size
    private static final int DEFAULT_CACHE_SIZE = 10000;

    /**
     * ???
     */
    private final Cache<Id, PersistentEObject> persistentObjectsCache;

    /**
     * ???
     */
    private final Cache<Id, Vertex> verticesCache;

    /**
     * ???
     */
    private final List<EClass> indexedEClasses;

    /**
     * ???
     */
    private final Index<Vertex> metaclassIndex;

    /**
     * The Blueprints database.
     */
    private final IdGraph<KeyIndexableGraph> graph;

    /**
     * Whether the underlying database is closed.
     */
    private boolean isClosed = false;

    /**
     * Constructs a new {@code BlueprintsPersistenceBackend} wrapping the provided {@code baseGraph}.
     * <p>
     * This constructor initialize the caches and create the metaclass index.
     *
     * @param baseGraph the base {@link KeyIndexableGraph} used to access the database
     *
     * @note This constructor is package-private. To create a new {@code BlueprintsPersistenceBackend} see {@link
     * BlueprintsPersistenceBackendFactory#createPersistentBackend(java.io.File, Map)}.
     * @see BlueprintsPersistenceBackendFactory
     */
    BlueprintsPersistenceBackend(KeyIndexableGraph baseGraph) {
        this.graph = new AutoCleanerIdGraph(baseGraph);
        this.persistentObjectsCache = Caffeine.newBuilder().maximumSize(DEFAULT_CACHE_SIZE).softValues().build();
        this.verticesCache = Caffeine.newBuilder().maximumSize(DEFAULT_CACHE_SIZE).softValues().build();
        this.indexedEClasses = new ArrayList<>();

        Index<Vertex> metaclasses = graph.getIndex(KEY_METACLASSES, Vertex.class);
        if (isNull(metaclasses)) {
            metaclassIndex = graph.createIndex(KEY_METACLASSES, Vertex.class);
        }
        else {
            metaclassIndex = metaclasses;
        }
    }

    /**
     * Builds the {@link Id} used to identify an {@link EClass} {@link Vertex}.
     *
     * @param eClass the {@link EClass} to build an {@link Id} from
     *
     * @return the create {@link Id}
     */
    private static Id buildId(EClass eClass) {
        return isNull(eClass) ? null : new StringId(eClass.getName() + '@' + eClass.getEPackage().getNsURI());
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void close() {
        try {
            graph.shutdown();
        }
        catch (Exception e) {
            NeoLogger.warn(e);
        }
        isClosed = true;
    }

    @Override
    public void save() {
        if (graph.getFeatures().supportsTransactions) {
            graph.commit();
        }
        else {
            graph.shutdown();
        }
    }

    @Override
    public Map<EClass, Iterable<Vertex>> getAllInstances(EClass eClass, boolean strict) {
        Map<EClass, Iterable<Vertex>> indexHits;

        // There is no strict instance of an abstract class
        if (eClass.isAbstract() && strict) {
            indexHits = Collections.emptyMap();
        }
        else {
            indexHits = new HashMap<>();
            Set<EClass> eClassToFind = new HashSet<>();
            eClassToFind.add(eClass);

            // Find all the concrete subclasses of the given EClass (the metaclass index only stores concretes EClass)
            if (!strict) {
                eClass.getEPackage().getEClassifiers()
                        .stream()
                        .filter(EClass.class::isInstance)
                        .map(EClass.class::cast)
                        .filter(c -> eClass.isSuperTypeOf(c) && !c.isAbstract())
                        .forEach(eClassToFind::add);
            }
            // Get all the vertices that are indexed with one of the EClass
            for (EClass ec : eClassToFind) {
                Vertex metaClassVertex = Iterables.getOnlyElement(metaclassIndex.get(KEY_NAME, ec.getName()), null);
                if (nonNull(metaClassVertex)) {
                    Iterable<Vertex> instanceVertexIterable = metaClassVertex.getVertices(Direction.IN, KEY_INSTANCE_OF);
                    indexHits.put(ec, instanceVertexIterable);
                }
                else {
                    NeoLogger.warn("Metaclass {0} not found in index", ec.getName());
                }
            }
        }
        return indexHits;
    }

    /**
     * Create a new vertex, add it to the graph, and return the newly created vertex.
     *
     * @param id the identifier of the {@link Vertex}
     *
     * @return the newly created vertex
     */
    public Vertex addVertex(Id id) {
        return graph.addVertex(id.toString());
    }

    /**
     * Create a new vertex, add it to the graph, and return the newly created vertex. The issued {@link EClass} is used
     * to calculate the {@link Vertex} {@code id}.
     *
     * @param eClass The corresponding {@link EClass}
     *
     * @return the newly created vertex
     */
    private Vertex addVertex(EClass eClass) {
        Vertex vertex = addVertex(buildId(eClass));
        vertex.setProperty(KEY_ECLASS_NAME, eClass.getName());
        vertex.setProperty(KEY_EPACKAGE_NSURI, eClass.getEPackage().getNsURI());
        return vertex;
    }

    /**
     * Returns the vertex corresponding to the provided {@code id}. If no vertex corresponds to that {@code id}, then
     * return {@code null}.
     *
     * @param id the {@link Id} of the element to find
     *
     * @return the vertex referenced by the provided {@link EObject} or {@code null} when no such vertex exists
     */
    public Vertex getVertex(Id id) {
        return verticesCache.get(id, key -> graph.getVertex(key.toString()));
    }

    /**
     * Returns the vertex corresponding to the provided {@link EClass}. If no vertex corresponds to that {@link EClass},
     * then return {@code null}.
     *
     * @param eClass the {@link EClass} to find
     *
     * @return the vertex corresponding to the provided {@link EClass} or {@code null} when no such vertex exists
     */
    private Vertex getVertex(EClass eClass) {
        return getVertex(buildId(eClass));
    }

    /**
     * Return the vertex corresponding to the provided {@link PersistentEObject}. If no vertex corresponds to that
     * {@link EObject}, then the corresponding {@link Vertex} together with its {@link #KEY_INSTANCE_OF} relationship is
     * created.
     *
     * @param object the {@link PersistentEObject} to find
     *
     * @return the vertex referenced by the provided {@link EObject} or {@code null} when no such vertex exists
     */
    public Vertex getOrCreateVertex(PersistentEObject object) {
        Vertex vertex;
        if (object.isMapped()) {
            vertex = getVertex(object.id());
        }
        else {
            vertex = createVertex(object);
        }
        return vertex;
    }

    /**
     * ???
     *
     * @param vertex ???
     * @param object ???
     */
    private void setMappedVertex(Vertex vertex, PersistentEObject object) {
        object.setMapped(true);
        persistentObjectsCache.put(object.id(), object);
        verticesCache.put(object.id(), vertex);
    }

    /**
     * ???
     *
     * @param vertex ???
     *
     * @return ???
     */
    private EClass resolveInstanceOf(Vertex vertex) {
        EClass eClass = null;
        Vertex eClassVertex = Iterables.getOnlyElement(vertex.getVertices(Direction.OUT, KEY_INSTANCE_OF), null);
        if (nonNull(eClassVertex)) {
            ClassInfo classInfo = ClassInfo.of(eClassVertex.getProperty(KEY_ECLASS_NAME), eClassVertex.getProperty(KEY_EPACKAGE_NSURI));
            eClass = classInfo.eClass();
        }
        return eClass;
    }

    /**
     * Reifies the given {@link Vertex} as a {@link PersistentEObject}
     * <p>
     * The method guarantees that the same {@link PersistentEObject} is returned for a given {@link Vertex} in
     * subsequent calls, unless the {@link PersistentEObject} returned in previous calls has been already garbage
     * collected. This method is a shortcut for {@link BlueprintsPersistenceBackend#reifyVertex(Vertex, EClass)} with a
     * {@code null} EClass.
     *
     * @param vertex the {@link Vertex} to reify
     *
     * @return a {@link PersistentEObject} representing the given vertex
     */
    public PersistentEObject reifyVertex(Vertex vertex) {
        return reifyVertex(vertex, null);
    }

    /**
     * Reifies the given {@link Vertex} as an {@link EObject}.
     * <p>
     * The method guarantees that the same {@link EObject} is returned for a given {@link Vertex} in subsequent calls,
     * unless the {@link EObject} returned in previous calls has been already garbage collected.
     *
     * @param vertex the {@link Vertex} to reify
     * @param eClass the expected {@link EClass} of the reified object. Can be set to {@code null} if not known.
     *
     * @return a {@link PersistentEObject} representing the given vertex
     */
    public PersistentEObject reifyVertex(Vertex vertex, EClass eClass) {
        PersistentEObject object = null;

        Id id = new StringId(vertex.getId().toString());
        if (isNull(eClass)) {
            eClass = resolveInstanceOf(vertex);
        }
        try {
            object = persistentObjectsCache.get(id, new PersistentEObjectCacheLoader(eClass));
        }
        catch (Exception e) {
            NeoLogger.error(e);
        }
        return object;
    }

    /**
     * ???
     *
     * @param object ???
     *
     * @return ???
     */
    private Vertex createVertex(PersistentEObject object) {
        Vertex vertex = addVertex(object.id());
        EClass eClass = object.eClass();

        Vertex eClassVertex = Iterables.getOnlyElement(metaclassIndex.get(KEY_NAME, eClass.getName()), null);
        if (isNull(eClassVertex)) {
            eClassVertex = addVertex(eClass);
            metaclassIndex.put(KEY_NAME, eClass.getName(), eClassVertex);
            indexedEClasses.add(eClass);
        }
        vertex.addEdge(KEY_INSTANCE_OF, eClassVertex);
        setMappedVertex(vertex, object);
        return vertex;
    }

    /**
     * Copies all the contents of this back-end to the target one.
     *
     * @param target the {@code BlueprintsPersistenceBackend} to copy the elements to
     */
    public void copyTo(BlueprintsPersistenceBackend target) {
        GraphHelper.copyGraph(graph, target.graph);
        target.initMetaClassesIndex(indexedEClasses);
    }

    /**
     * Provides a direct access to the underlying graph. This method is public for tool compatibility (see
     * <a href="https://github.com/atlanmod/Mogwai">the Mogwaï framework</a>), NeoEMF consistency is not guaranteed if
     * the graph is modified manually.
     *
     * @return the underlying Blueprints {@link IdGraph}
     */
    public IdGraph<KeyIndexableGraph> getGraph() {
        return graph;
    }

    /**
     * ???
     *
     * @param eClassList ???
     */
    private void initMetaClassesIndex(List<EClass> eClassList) {
        for (EClass eClass : eClassList) {
            checkArgument(Iterables.isEmpty(metaclassIndex.get(KEY_NAME, eClass.getName())), "Index is not consistent");
            metaclassIndex.put(KEY_NAME, eClass.getName(), getVertex(eClass));
        }
    }

    /**
     * ???
     */
    private static class PersistentEObjectCacheLoader implements Function<Id, PersistentEObject> {

        /**
         * The class associated with the object to retrieve.
         */
        private final EClass eClass;

        /**
         * Constructs a new {@code PersistentEObjectCacheLoader} with the given {@code eClass}.
         *
         * @param eClass the class associated with the object to retrieve
         */
        private PersistentEObjectCacheLoader(EClass eClass) {
            this.eClass = eClass;
        }

        @Override
        public PersistentEObject apply(Id id) {
            PersistentEObject object;
            if (nonNull(eClass)) {
                EObject eObject;
                if (Objects.equals(eClass.getEPackage().getClass(), EPackageImpl.class)) {
                    // Dynamic EMF
                    eObject = PersistenceFactory.getInstance().create(eClass);
                }
                else {
                    eObject = EcoreUtil.create(eClass);
                }
                object = PersistentEObject.from(eObject);
                object.id(id);
                object.setMapped(true);
            }
            else {
                throw new RuntimeException("Element " + id + " does not have an associated EClass");
            }
            return object;
        }
    }

    /**
     * ???
     */
    private static class AutoCleanerIdGraph extends IdGraph<KeyIndexableGraph> {

        /**
         * Constructs a new {@code AutoCleanerIdGraph} on the specified {@code baseGraph}.
         *
         * @param baseGraph the base graph
         */
        public AutoCleanerIdGraph(KeyIndexableGraph baseGraph) {
            super(baseGraph);
        }

        @Override
        public Edge addEdge(Object id, Vertex outVertex, Vertex inVertex, String label) {
            return createFrom(super.addEdge(id, outVertex, inVertex, label));
        }

        @Override
        public Edge getEdge(Object id) {
            return createFrom(super.getEdge(id));
        }

        /**
         * ???
         *
         * @param edge ???
         *
         * @return ???
         */
        private Edge createFrom(Edge edge) {
            return isNull(edge) ? null : new AutoCleanerIdEdge(edge);
        }

        /**
         * ???
         */
        private class AutoCleanerIdEdge extends IdEdge {

            /**
             * Constructs a new {@code AutoCleanerIdEdge} on the specified {@code edge}.
             *
             * @param edge the base edge
             */
            public AutoCleanerIdEdge(Edge edge) {
                super(edge, AutoCleanerIdGraph.this);
            }

            /**
             * {@inheritDoc}
             * <p>
             * If the {@link Edge} references a {@link Vertex} with no more incoming {@link Edge}, the referenced
             * {@link Vertex} is removed as well.
             */
            @Override
            public void remove() {
                Vertex referencedVertex = getVertex(Direction.IN);
                super.remove();
                if (Iterables.isEmpty(referencedVertex.getEdges(Direction.IN))) {
                    // If the Vertex has no more incoming edges remove it from the DB
                    referencedVertex.remove();
                }
            }
        }
    }
}