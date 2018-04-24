package fr.inria.atlanmod.neoemf.data.blueprints.store;

import com.google.common.collect.Iterables;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.core.PersistentEObject;
import fr.inria.atlanmod.neoemf.core.StringId;
import fr.inria.atlanmod.neoemf.data.blueprints.BlueprintsPersistenceBackend;
import fr.inria.atlanmod.neoemf.data.store.PersistentStore;
import fr.inria.atlanmod.neoemf.util.logging.NeoLogger;
import org.apache.commons.lang.ArrayUtils;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class DirectWriteBlueprintsStoreLongListSupport extends DirectWriteBlueprintsStore {

    public DirectWriteBlueprintsStoreLongListSupport(Resource.Internal resource, BlueprintsPersistenceBackend backend) {
        super(resource, backend);
    }

    @Override
    protected Object getReference(PersistentEObject object, EReference reference, int index) {
        Object referencedObject = null;
        Vertex vertex = backend.getVertex(object.id());
        VertexList vList = listFor(vertex, reference);
        Vertex referencedVertex = vList.get(index);
        if(nonNull(referencedVertex)) {
            referencedObject = reifyVertex(referencedVertex);
        }
        return referencedObject;
    }

    @Override
    protected Object setReference(PersistentEObject object, EReference reference, int index, PersistentEObject value) {
        Vertex vertex = backend.getOrCreateVertex(object);
        Vertex newReferencedVertex = backend.getOrCreateVertex(value);
        if(reference.isContainment()) {
            updateContainment(reference, vertex, newReferencedVertex);
        }

        VertexList vList = listFor(vertex, reference);
        Vertex oldVertex = vList.set(newReferencedVertex, index);

        Object old = null;
        if(nonNull(oldVertex)) {
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
        if(index == PersistentStore.NO_INDEX) {
            index = size(object, reference);
        }
        Vertex vertex = backend.getOrCreateVertex(object);
        Vertex referencedVertex = backend.getOrCreateVertex(value);

        if(reference.isContainment()) {
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
        if(reference.isContainment()) {
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
        vList.clear();;
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
        if(feature instanceof EReference) {
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
                    return (T[]) new Object[] { parseProperty((EAttribute) feature, property) };
                } else {
                    array[0] = (T) parseProperty((EAttribute) feature, property);
                    return array;
                }
            }
        }
    }

    @Override
    protected Integer getSize(Vertex vertex, EStructuralFeature feature) {
        if(feature instanceof EReference) {
            VertexList vList = listFor(vertex, (EReference) feature);
            return vList.size();
        } else {
            Integer size = vertex.getProperty(feature.getName() + SEPARATOR + SIZE_LITERAL);
            return isNull(size) ? 0 : size;
        }
    }

    private VertexList listFor(Vertex from, EReference reference) {
        VertexList list = null;
        Vertex listBase = Iterables.getOnlyElement(from.getVertices(Direction.OUT, reference.getName()), null);
        if(isNull(listBase)) {
            listBase = backend.addVertex(StringId.generate());
            from.addEdge(reference.getName(), listBase);
            list = new VertexList(listBase, backend, reference);
        } else {
            Vertex head = Iterables.getOnlyElement(listBase.getVertices(Direction.OUT, VertexList.HEAD), null);
            if(nonNull(head)) {
                Vertex tail = Iterables.getOnlyElement(listBase.getVertices(Direction.OUT, VertexList.TAIL), null);
                list = new VertexList(listBase, head, tail, backend, reference);
            } else {
                list = new VertexList(listBase, backend, reference);
            }
        }
        return list;
    }

    /**
     * Represent a multi-valued reference using a double-linked list represented with graph primitives.
     */
    private static class VertexList {

        protected static final String HEAD = "head";

        protected static final String TAIL = "tail";

        protected static final String NEXT = "next";

        protected static final String VALUE = "value";

        private BlueprintsPersistenceBackend backend;

        private Vertex base;

        private Vertex head;

        private Vertex tail;

        private String refLabel;

        private VertexList(Vertex base, Vertex head, Vertex tail, BlueprintsPersistenceBackend backend, EReference
                reference) {
            this.base = base;
            this.head = head;
            this.tail = tail;
            this.backend = backend;
            this.refLabel = VALUE + "_" + reference.getName();
        }

        private VertexList(Vertex base, BlueprintsPersistenceBackend backend, EReference reference) {
            this(base, null, null, backend, reference);
            setSize(0);
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

        // returns the value
        public Vertex set(Vertex v, int index) {
            int size = size();
            Vertex oldNode = null;
            if(index == InternalEObject.EStore.NO_INDEX) {
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

        public Vertex remove(int index) {
            int size = size();
            // TODO check if remove can be called on single-valued references.
            Vertex oldNode = getNodeAtIndex(index, size);
            Vertex oldNodeValue = getValue(oldNode);
            Vertex prevNode = getPrev(oldNode);
            Vertex nextNode = getNext(oldNode);
            if(isNull(prevNode)) {
                /*
                 * We are removing the head.
                 */
                setHead(nextNode);
            } else if(isNull(nextNode)) {
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
            setSize(size -1);
            return oldNodeValue;
        }

        public int indexOf(Id id) {
            int index = 0;
            // TODO check if we can improve here
            Vertex node = head;
            while(nonNull(node)) {
                if(Objects.equals(getValue(node).getId(), id)) {
                    return index;
                }
                index++;
            }
            return ArrayUtils.INDEX_NOT_FOUND;
        }

        public int lastIndexOf(Id id) {
            int size = size();
            int index = size - 1;
            Vertex node = tail;
            while(nonNull(tail)) {
                if(Objects.equals(getValue(node).getId(), id)) {
                    return index;
                }
                index--;
            }
            return ArrayUtils.INDEX_NOT_FOUND;
        }

        public boolean contains(Vertex vertex) {
            /*
             * Retrieve the node in the list referencing the element
             */
            Iterable<Vertex> refNodes = vertex.getVertices(Direction.IN, refLabel);
            if(Iterables.isEmpty(refNodes)) {
                /*
                 * The element is not referenced by any refLabel edge, it cannot be contained in the list.
                 */
                return false;
            }
            else {
                for(Vertex refNode : refNodes) {
                    /**
                     * Iterates all the lists and find if its tail is equal to the one representing this list.
                     */
                    Vertex node = refNode;
                    while(nonNull(node)) {
                        if(node.equals(tail)) {
                            return true;
                        }
                    }
                    node = getNext(node);
                }
            }
            // Should never happen?
            return false;
        }

        public <T> T[] toArray(T[] array) {
            int size = size();
            Object[] result = new Object[size];
            int index = 0;
            Vertex node = head;
            while(nonNull(node)) {
                // Reify here to avoid multiple iterations on the array
                // Not clean, we should not manipulate EObjects in this class
                result[index] = backend.reifyVertex(getValue(node));
                index++;
            }
            if(isNull(array)) {
                return (T[]) result;
            } else {
                System.arraycopy(result, 0, array, 0, result.length);
                return array;
            }
        }

        public void clear() {
            // Does not remove the nodes in the list !
            base.getEdges(Direction.BOTH, HEAD, TAIL).forEach(e -> e.remove());
            setSize(0);
        }

        /**
         * Returns the size of the internal linked list.
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
            checkElementIndex(index, size);
            Vertex node = null;
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
    }
}
