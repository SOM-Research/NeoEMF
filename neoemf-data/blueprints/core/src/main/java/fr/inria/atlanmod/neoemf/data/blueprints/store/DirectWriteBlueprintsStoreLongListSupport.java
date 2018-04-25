package fr.inria.atlanmod.neoemf.data.blueprints.store;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import fr.inria.atlanmod.neoemf.core.StringId;
import fr.inria.atlanmod.neoemf.data.blueprints.BlueprintsPersistenceBackend;
import fr.inria.atlanmod.neoemf.data.store.AbstractPersistentStoreDecorator;
import fr.inria.atlanmod.neoemf.data.store.PersistentStore;
import fr.inria.atlanmod.neoemf.util.logging.NeoLogger;
import org.apache.commons.lang.ArrayUtils;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A {@link fr.inria.atlanmod.neoemf.data.store.DirectWriteStore} that translates model-level operation to Blueprints
 * calls, with additional support for long feature lists.
 * <p>
 * This class relies on an in-database linked list to represent feature collections, speeding-up addition and
 * deletion of elements. When possible, lookup methods such as {@code contains} and {@code indexOf} are optimized to
 * avoid full iteration of the underlying list.
 * <p>
 * This class implements the {@link PersistentStore} interface that defines a set of operations to implement in order to
 * allow EMF persistence delegation. If this store is used, every method call and property access on {@link
 * PersistentEObject} is forwarded to this class, that takes care of the database serialization and deserialization
 * using its embedded {@link BlueprintsPersistenceBackend}.
 * <p>
 * This store can be used as a base store that can be complemented by plugging decorator stores on top of it (see {@link
 * AbstractPersistentStoreDecorator} subclasses) to provide additional features such as caching or logging.
 *
 * @see PersistentEObject
 * @see BlueprintsPersistenceBackend
 * @see AbstractPersistentStoreDecorator
 */
public class DirectWriteBlueprintsStoreLongListSupport extends DirectWriteBlueprintsStore {

    /**
     * Constructs a new {@code DirectWriteBlueprintsStoreLongListSupport} between the given {@code resource} and the
     * {@code backend}.
     *
     * @param resource the resource to persist and access
     * @param backend  the persistence back-end to store the model
     */
    public DirectWriteBlueprintsStoreLongListSupport(Resource.Internal resource, BlueprintsPersistenceBackend backend) {
        super(resource, backend);
    }

    @Override
    protected Object getReference(PersistentEObject object, EReference reference, int index) {
        Object referencedObject = null;
        Vertex vertex = backend.getVertex(object.id());
        VertexList vList = listFor(vertex, reference);
        Vertex referencedVertex = vList.get(index);
        if (nonNull(referencedVertex)) {
            referencedObject = reifyVertex(referencedVertex);
        }
        return referencedObject;
    }

    @Override
    protected Object setReference(PersistentEObject object, EReference reference, int index, PersistentEObject value) {
        Vertex vertex = backend.getOrCreateVertex(object);
        Vertex newReferencedVertex = backend.getOrCreateVertex(value);
        if (reference.isContainment()) {
            updateContainment(reference, vertex, newReferencedVertex);
        }

        VertexList vList = listFor(vertex, reference);
        Vertex oldVertex = vList.set(newReferencedVertex, index);

        Object old = null;
        if (nonNull(oldVertex)) {
            old = reifyVertex(oldVertex);
        }
        return old;
    }

    @Override
    protected void unsetReference(PersistentEObject object, EReference reference) {
        // TODO check if this is correct
        Vertex vertex = backend.getVertex(object.id());
        VertexList vList = listFor(vertex, reference);
        vList.clear();
    }

    @Override
    protected boolean containsReference(PersistentEObject object, EReference reference, PersistentEObject value) {
        Vertex vertex = backend.getOrCreateVertex(object);
        Vertex referencedVertex = backend.getOrCreateVertex(object);
        VertexList vList = listFor(vertex, reference);
        return vList.contains(referencedVertex);
    }

    @Override
    protected int indexOfReference(PersistentEObject object, EReference reference, PersistentEObject value) {
        Vertex vertex = backend.getVertex(object.id());
        VertexList vList = listFor(vertex, reference);
        return vList.indexOf(value.id());
    }

    @Override
    protected int lastIndexOfReference(PersistentEObject object, EReference reference, PersistentEObject value) {
        Vertex vertex = backend.getVertex(object.id());
        VertexList vList = listFor(vertex, reference);
        return vList.lastIndexOf(value.id());
    }

    @Override
    protected void addReference(PersistentEObject object, EReference reference, int index, PersistentEObject value) {
        if (index == PersistentStore.NO_INDEX) {
            index = size(object, reference);
        }
        Vertex vertex = backend.getOrCreateVertex(object);
        Vertex referencedVertex = backend.getOrCreateVertex(value);

        if (reference.isContainment()) {
            updateContainment(reference, vertex, referencedVertex);
        }

        VertexList vList = listFor(vertex, reference);
        vList.add(referencedVertex, index);
    }

    @Override
    protected Object removeReference(PersistentEObject object, EReference reference, int index) {
        Vertex vertex = backend.getVertex(object.id());
        VertexList vList = listFor(vertex, reference);
        Vertex oldVertex = vList.remove(index);
        checkNotNull(oldVertex);
        InternalEObject oldObject = reifyVertex(oldVertex);
        if (reference.isContainment()) {
            oldObject.eBasicSetContainer(null, -1, null);
            ((PersistentEObject) oldObject).resource(null);
        }
        return oldObject;
    }

    @Override
    protected void clearReference(PersistentEObject object, EReference reference) {
        // TODO check if this is correct regarding VertexList.clear() implementation
        Vertex vertex = backend.getOrCreateVertex(object);
        VertexList vList = listFor(vertex, reference);
        vList.clear();
        ;
    }

    @Override
    public Object[] toArray(InternalEObject internalObject, EStructuralFeature feature) {
        return toArray(internalObject, feature, null);
    }

    @Override
    public <T> T[] toArray(InternalEObject internalObject, EStructuralFeature feature, T[] array) {
        checkArgument(feature instanceof EReference || feature instanceof EAttribute, "Cannot compute toArray from " +
                "feature {0}: unknown EStructuralFeature type {1}", feature.getName(), feature.getClass()
                .getSimpleName());
        PersistentEObject object = PersistentEObject.from(internalObject);
        Vertex vertex = backend.getVertex(object.id());
        if (feature instanceof EReference) {
            VertexList vList = listFor(vertex, (EReference) feature);
            return vList.toArray(array);
        } else {
            String propertyName = feature.getName();
            if (feature.isMany()) {
                int size = getSize(vertex, feature);
                T[] output = array;
                if (isNull(array)) {
                    output = (T[]) new Object[size];
                }
                for (int i = 0; i < size; i++) {
                    Object parsedProperty = parseProperty((EAttribute) feature,
                            vertex.getProperty(propertyName + SEPARATOR + i));
                    output[i] = (T) parsedProperty;
                }
                // Return array if it as been provided to ensure the reference
                // does not change
                return isNull(array) ? output : array;
            } else {
                Object property = vertex.getProperty(propertyName);
                if (isNull(array)) {
                    return (T[]) new Object[]{parseProperty((EAttribute) feature, property)};
                } else {
                    array[0] = (T) parseProperty((EAttribute) feature, property);
                    return array;
                }
            }
        }
    }

    @Override
    protected Integer getSize(Vertex vertex, EStructuralFeature feature) {
        if (feature instanceof EReference) {
            VertexList vList = listFor(vertex, (EReference) feature);
            return vList.size();
        } else {
            Integer size = vertex.getProperty(feature.getName() + SEPARATOR + SIZE_LITERAL);
            return isNull(size) ? 0 : size;
        }
    }

    @Override
    protected InternalEObject reifyVertex(Vertex vertex, @Nullable EClass eClass) {
        PersistentEObject internalEObject = backend.reifyVertex(vertex, eClass);
        if (internalEObject.resource() != resource()) {
            if (Iterables.isEmpty(vertex.getEdges(Direction.OUT, CONTAINER))) {
                if (!Iterables.isEmpty(vertex.getVertices(Direction.IN, VertexList.VALUE + "_" + CONTENTS))) {
                    internalEObject.resource(resource());
                }
                // else : not part of the resource
            } else {
                internalEObject.resource(resource());
            }
        }
        return internalEObject;
    }

    /**
     * The last {@link VertexList} that has been retrieved.
     * <p>
     * This cache speeds-up {@link #listFor(Vertex, EReference)} computation when accessing the same list multiple
     * times (e.g. in a complete feature traversal).
     *
     * @see #listFor(Vertex, EReference) 
     */
    private VertexList previousVertexList = null;

    /**
     * Creates a {@link VertexList} representing the provided {@code reference} associated to the given element
     * represented by {@code from}.
     * <p>
     * This method creates an instance of the linked list wrapper that allows to easily manipulate collections of
     * elements stored in the provided {@code reference}. Collections should be manipulated using this wrapper to
     * ensure that the underlying list stays consistent.
     *
     * @param from      the {@link Vertex} representing the element to get the list of
     * @param reference the {@link EReference} representing the feature content to access
     * @return the created {@link VertexList}
     * @see VertexList
     */
    private VertexList listFor(Vertex from, EReference reference) {
        if (nonNull(previousVertexList)) {
            if (previousVertexList.getFrom().equals(from) && previousVertexList.getEReference().equals(reference)) {
                return previousVertexList;
            }
        }
        VertexList list = null;
        Vertex listBase = Iterables.getOnlyElement(from.getVertices(Direction.OUT, reference.getName()), null);
        if (isNull(listBase)) {
            listBase = backend.addVertex(StringId.generate());
            from.addEdge(reference.getName(), listBase);
            list = new VertexList(from, listBase, backend, reference);
        } else {
            Vertex head = Iterables.getOnlyElement(listBase.getVertices(Direction.OUT, VertexList.HEAD), null);
            if (nonNull(head)) {
                Vertex tail = Iterables.getOnlyElement(listBase.getVertices(Direction.OUT, VertexList.TAIL), null);
                list = new VertexList(from, listBase, head, tail, backend, reference);
            } else {
                list = new VertexList(from, listBase, backend, reference);
            }
        }
        previousVertexList = list;
        return list;
    }

    /**
     * Represent a multi-valued reference using a double-linked list represented with graph primitives.
     */
    private static class VertexList {

        /**
         * The {@link Edge} label used to represent the head of the list.
         */
        protected static final String HEAD = "head";

        /**
         * The {@link Edge} label used to represent the tail of the list.
         */
        protected static final String TAIL = "tail";

        /**
         * The {@link Edge} label used to represent the next element in the list.
         * <p>
         * Note that the list does not provide a {@code previous} edge label, because Blueprints edges can be
         * navigated forward and backward.
         */
        protected static final String NEXT = "next";

        /**
         * The {@link Edge} label used to link list vertices to their concrete values.
         */
        protected static final String VALUE = "value";

        /**
         * The {@link BlueprintsPersistenceBackend} used to create new list vertices.
         */
        private BlueprintsPersistenceBackend backend;

        /**
         * The element {@link Vertex} to retrieve the list from.
         * <p>
         * The element {@link Vertex} stores element-related information such as attributes and references, and is
         * linked to the {@link #base} {@link Vertex} representing this list.
         */
        private Vertex from;

        /**
         * The base {@link Vertex} of the list.
         * <p>
         * This {@link Vertex} is used to limit the number of outgoing edges of a given element {@link Vertex}: an
         * element can contain n {@code base} vertices, where n is the number of possible features of the element.
         * This avoid costly edge lookups when iterating features.
         */
        private Vertex base;

        /**
         * The head {@link Vertex} of the list.
         *
         * @see #tail
         */
        private Vertex head;

        /**
         * The tail {@link Vertex} of the list.
         *
         * @see #head
         */
        private Vertex tail;

        /**
         * The edge label used to represent the feature content.
         */
        private String refLabel;

        /**
         * The {@link EReference} represented by this list.
         */
        private EReference eReference;

        /**
         * The last indexed {@link Vertex} retrieved by this list.
         * <p>
         * This value allows to speed-up list traversal for continuous iterations (i.e. from n to m), by avoiding
         * costly iterations of the list.
         */
        private IndexedVertex lastIndexedVertex = null;

        /**
         * Constructs a new {@link VertexList} with the provided {@code base}, {@code head}, {@code tail}, managed by
         * the provided {@code backend}, and representing the given {@code reference}.
         * <p>
         * This constructor is used to create {@link VertexList} instances representing an existing collection. To
         * create an empty {@link VertexList} see
         * {@link #VertexList(Vertex, Vertex, BlueprintsPersistenceBackend, EReference)}.
         *
         * @param from      the element {@link Vertex}
         * @param base      the base {@link Vertex} of the list
         * @param head      the head {@link Vertex} of the list
         * @param tail      the tail {@link Vertex} of the list
         * @param backend   the {@link BlueprintsPersistenceBackend} used to create new list vertices
         * @param reference the element's feature to construct the list for
         * @see #VertexList(Vertex, Vertex, BlueprintsPersistenceBackend, EReference)
         */
        private VertexList(Vertex from, Vertex base, Vertex head, Vertex tail, BlueprintsPersistenceBackend backend,
                           EReference reference) {
            this.from = from;
            this.base = base;
            this.head = head;
            this.tail = tail;
            this.backend = backend;
            this.eReference = reference;
            this.refLabel = VALUE + "_" + reference.getName();
        }

        /**
         * Constructs an empty {@link VertexList} with the provided {@code base}, managed by the provided {@code
         * backend}, and representing the given {@code reference}.
         * <p>
         * This constructor sets the {@link #head}, and {@link #tail} vertices to {@code null}. To create a
         * {@link VertexList} from an existing list see
         * {@link #VertexList(Vertex, Vertex, Vertex, Vertex, BlueprintsPersistenceBackend, EReference)}.
         *
         * @param from      the element {@link Vertex}
         * @param base      the base {@link Vertex} of the list
         * @param backend   the {@link BlueprintsPersistenceBackend} used to create new list vertices
         * @param reference the element's feature to construct the list for
         * @see #VertexList(Vertex, Vertex, BlueprintsPersistenceBackend, EReference)
         */
        private VertexList(Vertex from, Vertex base, BlueprintsPersistenceBackend backend, EReference reference) {
            this(from, base, null, null, backend, reference);
            setSize(0);
        }

        /**
         * Returns the {@link #from} {@link Vertex} of this list.
         *
         * @return the {@link #from} {@link Vertex} of this list
         */
        public Vertex getFrom() {
            return from;
        }

        /**
         * Returns the {@link #eReference} represented by this list.
         *
         * @return the {@link #eReference} represented by this list
         */
        public EReference getEReference() {
            return eReference;
        }

        /**
         * Retrieves the {@link Vertex} representing the model element stored at the given {@code index}.
         *
         * @param index the index of the element to retrieve
         * @return the {@link Vertex} representing the model element stored at the given {@code index}
         */
        public Vertex get(int index) {
            int size = size();
            if (size == 0) {
                return null;
            }
            Vertex node = null;
            if (index == InternalEObject.EStore.NO_INDEX) {
                /*
                 * The list represents a single-valued reference.
                 */
                node = getNodeAtIndex(0, size);
            } else {
                node = getNodeAtIndex(index, size);
            }
            return getValue(node);
        }

        /**
         * Adds the element represented by {@code v} at the given {@code index} to the list.
         * <p>
         * This method creates a new {@link Vertex} in the list, puts it at the provided {@code index}, and link it
         * to the provided {@code v}.
         *
         * @param v     the {@link Vertex} representing the element to add
         * @param index the position of the element to add
         * @see #setHead(Vertex)
         * @see #setTail(Vertex)
         */
        public void add(Vertex v, int index) {
            /*
             * Create the node in the list associated to the provided vertex.
             * TODO do we need to generate a new ID for the node vertex?
             */
            Vertex newNode = backend.addVertex(StringId.generate());
            newNode.addEdge(refLabel, v);
            int size = size();

            if (index == 0) {
                setHead(newNode);
            } else if (index == size) {
                setTail(newNode);
            } else {
                Vertex oldNode = getNodeAtIndex(index, size);
                Edge inNext = Iterables.getOnlyElement(oldNode.getEdges(Direction.IN, NEXT), null);
                Vertex inNode = inNext.getVertex(Direction.OUT);
                if (inNode.equals(oldNode)) {
                    // Debug
                    throw new RuntimeException("add: in/out error");
                }
                inNext.remove();
                inNode.addEdge(NEXT, newNode);
                newNode.addEdge(NEXT, oldNode);
            }
            setSize(size + 1);
        }

        /**
         * Sets the element represented by {@code v} at the given {@code index} to the list.
         * <p>
         * This method does not change the list itself, and reuses existing vertices to set the new value. Note that
         * the returned {@link Vertex} represent the removed element, not the list's updated vertex.
         *
         * @param v     the {@link Vertex} representing the element to set
         * @param index the position of the element to set
         * @return a {@link Vertex} representing the removed element
         * @see #getNodeAtIndex(int, int)
         * @see #getValue(Vertex)
         */
        public Vertex set(Vertex v, int index) {
            int size = size();
            Vertex oldNode = null;
            if (index == InternalEObject.EStore.NO_INDEX) {
                /*
                 * The list represents a single-valued reference.
                 */
                if (size == 0) {
                    add(v, 0);
                    return null;
                } else {
                    oldNode = getNodeAtIndex(0, size);
                }
            } else {
                oldNode = getNodeAtIndex(index, size);
            }
            Vertex oldNodeValue = getValue(oldNode);
            /*
             * This should be iterated once, each node has a single link to its value.
             */
            oldNode.getEdges(Direction.OUT, refLabel).forEach(e -> e.remove());
            /*
             * Change the link of the existing node to the new value.
             */
            oldNode.addEdge(refLabel, v);
            return oldNodeValue;
        }

        /**
         * Removes the element at the given {@code index} from the list.
         * <p>
         * This method removes the list's {@link Vertex} at the given {@code index}, and reset its incoming and
         * outgoing {@link Edge}s to ensure list consistency.
         *
         * @param index the position of the element to remove
         * @return a {@link Vertex} representing the removed element
         * @see #setHead(Vertex)
         * @see #setTail(Vertex)
         * @see #getValue(Vertex)
         */
        public Vertex remove(int index) {
            int size = size();
            // TODO check if remove can be called on single-valued references.
            Vertex oldNode = getNodeAtIndex(index, size);
            Vertex oldNodeValue = getValue(oldNode);
            Vertex prevNode = getPrev(oldNode);
            Vertex nextNode = getNext(oldNode);
            if (isNull(prevNode)) {
                /*
                 * We are removing the head.
                 */
                setHead(nextNode);
            } else if (isNull(nextNode)) {
                /*
                 * We are removing the tail.
                 */
                setTail(prevNode);
            } else {
                prevNode.addEdge(NEXT, nextNode);
            }
            /*
             * Delete the edges associated to the removed node and delete it.
             */
            oldNode.getEdges(Direction.BOTH, NEXT, refLabel, HEAD, TAIL).forEach(e -> e.remove());
            oldNode.remove();
            setSize(size - 1);
            return oldNodeValue;
        }

        /**
         * Returns the position of the element {@link Vertex} with the provided {@code id}.
         * <p>
         * This method iterates the list from the {@code head}, and returns the position of the first element
         * matching the provided {@code id}. To retrieve the last element with the provided {@code id}, see
         * {@link #lastIndexOf(Id)}.
         *
         * @param id the {@link Id} of the element {@link Vertex} to retrieve the position of
         * @return the position of the first element matching the provided {@code id}
         * @see #lastIndexOf(Id)
         * @see #getNext(Vertex)
         * @see #getValue(Vertex)
         */
        public int indexOf(Id id) {
            int index = 0;
            // TODO check if we can improve here
            Vertex node = head;
            while (nonNull(node)) {
                if (Objects.equals(getValue(node).getId(), id)) {
                    return index;
                }
                index++;
                node = getNext(node);
            }
            return ArrayUtils.INDEX_NOT_FOUND;
        }

        /**
         * Returns the position of the last element {@link Vertex} with the provided {@code id}.
         * <p>
         * This method iterates the list from the {@code tail}, and returns the position of the last element matching
         * the provided {@code id}. To retrieve the first element with the provided {@code if}, see
         * {@link #indexOf(Id)}.
         *
         * @param id the {@link Id} of the element {@link Vertex} to retrieve the position of
         * @return the position of the last element matching the provided {@code id}
         * @see #indexOf(Id)
         * @see #getPrev(Vertex)
         * @see #getValue(Vertex)
         */
        public int lastIndexOf(Id id) {
            int size = size();
            int index = size - 1;
            Vertex node = tail;
            while (nonNull(tail)) {
                if (Objects.equals(getValue(node).getId(), id)) {
                    return index;
                }
                index--;
                node = getPrev(node);
            }
            return ArrayUtils.INDEX_NOT_FOUND;
        }

        /**
         * Returns {@code true} if the list contains the provided element's {@code vertex}, {@code false} otherwise.
         * <p>
         * This implementation navigates backward the incoming {@link Edge}s of the provided {@code vertex} and
         * searches for the ones labeled {@link #refLabel}, and navigates until the tail of the list to compare it
         * with the {@link #tail} {@link Vertex}. This approach allows to fastly returns {@code false} when an
         * element is not contained, speeding-up the computation of element addition to EMF unique collections.
         *
         * @param vertex the element {@link Vertex} to check
         * @return {@code true} if the list contains the provided element's {@code vertex}, {@code false} otherwise
         * @see #getNext(Vertex)
         */
        public boolean contains(Vertex vertex) {
            /*
             * Retrieve the node in the list referencing the element
             */
            Iterable<Vertex> refNodes = vertex.getVertices(Direction.IN, refLabel);
            if (Iterables.isEmpty(refNodes)) {
                /*
                 * The element is not referenced by any refLabel edge, it cannot be contained in the list.
                 */
                return false;
            } else {
                for (Vertex refNode : refNodes) {
                    /**
                     * Iterates all the lists and find if its tail is equal to the one representing this list.
                     */
                    Vertex node = refNode;
                    while (nonNull(node)) {
                        if (node.equals(tail)) {
                            return true;
                        }
                    }
                    node = getNext(node);
                }
            }
            /*
             * The element is in a list representing the correct reference, but not this one (associated to another
             * element vertex).
             */
            return false;
        }

        /**
         * Returns an array representing the list contents.
         * <p>
         * This method fills the provided {@code array} if it is not {@code null}, or returns a new one otherwise.
         *
         * @param array the array to fill with the list contents
         * @param <T>   the type of the array's elements
         * @return an array representing the list contents
         */
        public <T> T[] toArray(T[] array) {
            int size = size();
            Object[] result = new Object[size];
            int index = 0;
            Vertex node = head;
            while (nonNull(node)) {
                // Reify here to avoid multiple iterations on the array
                // Not clean, we should not manipulate EObjects in this class
                result[index] = backend.reifyVertex(getValue(node));
                index++;
            }
            if (isNull(array)) {
                return (T[]) result;
            } else {
                System.arraycopy(result, 0, array, 0, result.length);
                return array;
            }
        }

        /**
         * Clears the list contents.
         * <p>
         * This method removes the {@link #head}, {@link #tail}, and {@link #base} vertices, as well as all the
         * intermediate vertices used to represent list elements.
         * <p>
         * <b>Note:</b> the current implementation only removes {@link #head}, {@link #tail}, and {@link #base}
         * edges, and not the intermediate list vertices
         * (<a href="https://github.com/SOM-Research/NeoEMF/issues/1">GitHub Issue</a>).
         */
        public void clear() {
            // Does not remove the nodes in the list !
            base.getEdges(Direction.BOTH, HEAD, TAIL).forEach(e -> e.remove());
            setSize(0);
        }

        /**
         * Returns the size of the internal linked list.
         * <p>
         * The size of the list is stored in a dedicated {@link #base} property to avoid costly iteration over the
         * list elements.
         *
         * @return the size of the internal linked list
         */
        public int size() {
            return base.getProperty(DirectWriteBlueprintsStoreLongListSupport.SIZE_LITERAL);
        }

        /**
         * Sets the size of the internal linked list to {@code newSize}.
         * <p>
         * This method is used to update the {@link Vertex} property representing the size of the list. Using such a
         * property instead of iterating the list to compute the size speeds-up computation.
         *
         * @param newSize the new size of the internal linked list
         */
        private void setSize(int newSize) {
            base.setProperty(DirectWriteBlueprintsStoreLongListSupport.SIZE_LITERAL, newSize);
        }

        /**
         * Retrieves the node in the linked list at the given {@code index}.
         * <p>
         * This method relies on the {@code head} and {@code tail} element to speed-up node retrieval: if the
         * provided {@code index} is in the first half of the list nodes are iterated from the {@code head}, on the
         * contrary, if the provided {@code index} is in the second half of the list nodes are iterated from the
         * {@code tail}.
         * <p>
         * The complexity of this method is O(n/2).
         *
         * @param index the index of the node to retrieve
         * @param size  the size of the list
         * @return a {@link Vertex} representing the retrieved node
         */
        private Vertex getNodeAtIndex(int index, int size) {
            // TODO refactor this method, not easy to understand
            checkElementIndex(index, size);
            Vertex node = null;
            if (nonNull(lastIndexedVertex)) {
                int previousIndex = lastIndexedVertex.getIndex();
                if (Math.abs(previousIndex - index) < index && Math.abs(previousIndex - index) < Math.abs(index - (size
                        - 1))) {
                    node = lastIndexedVertex.getVertex();
                    if (previousIndex > index) {
                        for (int i = previousIndex; i > index; i--) {
                            node = getPrev(node);
                        }
                    } else {
                        for (int i = previousIndex; i < index; i++) {
                            node = getNext(node);
                        }
                    }
                    lastIndexedVertex = new IndexedVertex(index, node);
                    return node;
                }
            }
            if (index < size / 2) {
                node = head;
                for (int i = 0; i < index; i++) {
                    node = getNext(node);
                }
            } else {
                node = tail;
                for (int i = 0; i < (size - 1) - index; i++) {
                    node = getPrev(node);
                }
            }
            if (isNull(node)) {
                NeoLogger.error("getNodeAtIndex({0}, {1}) returned null", index, size);
            }
            lastIndexedVertex = new IndexedVertex(index, node);
            return node;
        }

        /**
         * Returns the node after {@code from} from the linked list.
         *
         * @param from the {@link Vertex} representing the node in the list to retrieve the next element of
         * @return a {@link Vertex} representing the node after {@code from} from the linked list
         */
        private Vertex getNext(Vertex from) {
            Vertex next = Iterables.getOnlyElement(from.getVertices(Direction.OUT, NEXT), null);
            if (isNull(next)) {
                NeoLogger.error("getNext({0}) returned null", from);
            }
            return next;
        }

        /**
         * Returns the node before {@code from} from the linked list.
         *
         * @param from the {@link Vertex} representing the node in the list to retrieve the previous element of
         * @return a {@link Vertex} representing the node before {@code from} from the linked list
         */
        private Vertex getPrev(Vertex from) {
            Vertex prev = Iterables.getOnlyElement(from.getVertices(Direction.IN, NEXT), null);
            if (isNull(prev)) {
                NeoLogger.error("getPrev({0}) returned null", from);
            }
            return prev;
        }

        /**
         * Returns the {@link Vertex} representing the model element associated to the provided {@code from} node in
         * the linked list.
         *
         * @param from the {@link Vertex} representing the node in the list to retrieve the value of
         * @return the {@link Vertex} representing the model element associated to the provided {@code from} node in
         * the linked list
         */
        private Vertex getValue(Vertex from) {
            Vertex value = Iterables.getOnlyElement(from.getVertices(Direction.OUT, refLabel), null);
            if (isNull(value)) {
                NeoLogger.error("getValue({0}) returned null", from);
            }
            return value;
        }

        /**
         * Sets {@code newHead} as the new head of the linked list.
         *
         * @param newHead the new head of the linked list
         */
        private void setHead(Vertex newHead) {
            if (isNull(head)) {
                /*
                 * The list is empty, setting the head and tail to the {@code newHead} value.
                 */
                head = newHead;
                tail = newHead;
                /*
                 * Remove any existing link to a previous TAIL element and reset it to the new tail.
                 */
                Edge previousTailLink = Iterables.getOnlyElement(base.getEdges(Direction.OUT, TAIL), null);
                if (nonNull(previousTailLink)) {
                    NeoLogger.error("setHead on empty list: the previous tail is not null");
                    previousTailLink.remove();
                }
                base.addEdge(TAIL, tail);
            } else {
                /*
                 * Add a NEXT edge between the previous head and the new one, and set the head attribute to its new
                 * value.
                 */
                newHead.addEdge(NEXT, head);
                head = newHead;
            }
            /*
             * Remove any existing link between the base node and the previous head, and create a new one pointing to
             * the new head.
             */
            Edge previousHeadLink = Iterables.getOnlyElement(base.getEdges(Direction.OUT, HEAD), null);
            if (nonNull(previousHeadLink)) {
                previousHeadLink.remove();
            }
            base.addEdge(HEAD, head);
        }

        /**
         * Sets {@code newTail} as the new tail of the linked list.
         *
         * @param newTail the new tail of the linked list
         */
        private void setTail(Vertex newTail) {
            if (isNull(tail)) {
                /*
                 * The list is empty, setting the head and tail to the {@code newTail} value.
                 */
                tail = newTail;
                head = newTail;
                /*
                 * Remove any existing link to a previous HEAD element and reset it to the new tail.
                 */
                Edge previousHeadLink = Iterables.getOnlyElement(base.getEdges(Direction.OUT, HEAD), null);
                if (nonNull(previousHeadLink)) {
                    previousHeadLink.remove();
                }
                base.addEdge(HEAD, head);
            } else {
                /*
                 * Add a NEXT edge between the previous tail and the new one, and set the tail attribute to its new
                 * value.
                 */
                tail.addEdge(NEXT, newTail);
                /**
                 * Remove any existing link between the base node and the previous tail.
                 * TODO check why this is not done at the same location than for setHead
                 */
                Edge previousTailLink = Iterables.getOnlyElement(base.getEdges(Direction.OUT, TAIL), null);
                if (nonNull(previousTailLink)) {
                    previousTailLink.remove();
                }
                tail = newTail;
            }
            base.addEdge(TAIL, tail);
        }

        /**
         * A pair representing the last indexed {@link Vertex} by this list.
         * <p>
         * This class is used to speed-up computation of list traversal by caching the previously accessed
         * {@link Vertex} and its index. Using this information, the list can retrieve the next element by navigating
         * a single {@link Edge} instead of navigating from the {@link #head} or the {@link #tail} of the list.
         *
         * @see #getNodeAtIndex(int, int)
         */
        private static class IndexedVertex {

            /**
             * The index of the {@link Vertex} in the list.
             */
            private int index;

            /**
             * The last accessed {@link Vertex} in the list.
             */
            private Vertex vertex;

            /**
             * Constructs a new {@link IndexedVertex} with the provided {@code index} and {@code vertex}.
             *
             * @param index  the index of the {@code vertex} in the list
             * @param vertex the last accessed {@link Vertex} in the list
             */
            private IndexedVertex(int index, Vertex vertex) {
                this.index = index;
                this.vertex = vertex;
            }

            /**
             * Returns the index of the stored {@link Vertex} in the list.
             *
             * @return the index of the last accessed {@link Vertex} in the list
             */
            public int getIndex() {
                return index;
            }

            /**
             * Returns the {@link Vertex}.
             * @return the {@link Vertex}
             */
            public Vertex getVertex() {
                return vertex;
            }
        }
    }
}
