/*******************************************************************************
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 *******************************************************************************/

package fr.inria.atlanmod.neoemf.graph.blueprints.datastore.estores.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import fr.inria.atlanmod.neoemf.core.Id;
import fr.inria.atlanmod.neoemf.core.impl.NeoEObjectAdapterFactoryImpl;
import fr.inria.atlanmod.neoemf.datastore.InternalPersistentEObject;
import fr.inria.atlanmod.neoemf.datastore.estores.SearcheableResourceEStore;
import fr.inria.atlanmod.neoemf.graph.blueprints.datastore.BlueprintsPersistenceBackend;
import fr.inria.atlanmod.neoemf.logger.NeoLogger;

public class DirectWriteBlueprintsResourceEStoreImpl implements SearcheableResourceEStore {

	protected static final String SEPARATOR = ":";
	protected static final String POSITION = "position";
	protected static final String SIZE_LITERAL = "size";

	protected static final String CONTAINER = "eContainer";
	protected static final String CONTAINING_FEATURE = "containingFeature";

	protected BlueprintsPersistenceBackend graph;
	protected Resource.Internal resource;

	public DirectWriteBlueprintsResourceEStoreImpl(Resource.Internal resource, BlueprintsPersistenceBackend graph) {
		this.graph = graph;
		this.resource = resource;
        NeoLogger.log(NeoLogger.SEVERITY_INFO, "DirectWrite Store Created");
	}

	@Override
	public Object get(InternalEObject object, EStructuralFeature feature, int index) {
		if (feature instanceof EAttribute) {
			return get(object, (EAttribute) feature, index);
		} else if (feature instanceof EReference) {
			return get(object, (EReference) feature, index);
		} else {
			throw new IllegalArgumentException(feature.toString());
		}
	}

	protected Object get(InternalEObject object, EAttribute eAttribute, int index) {
		Vertex vertex = graph.getVertex(object);
		if (!eAttribute.isMany()) {
			Object property = vertex.getProperty(eAttribute.getName());
			return parseProperty(eAttribute, property);
		} else {
			Integer size = getSize(vertex, eAttribute);
			if (index < 0 || index >= size) {
			    NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Invalid get index " + index);
				throw new IndexOutOfBoundsException("Invalid get index " + index);
			} else {
				Object property = vertex.getProperty(eAttribute.getName() + SEPARATOR + index);
				return parseProperty(eAttribute, property);
			}
		}
	}

	protected Object get(InternalEObject object, EReference eReference, int index) {
		Vertex vertex = graph.getVertex(object);
		if (!eReference.isMany()) {
			Iterator<Vertex> iterator = vertex.getVertices(Direction.OUT, eReference.getName()).iterator();
			if (iterator.hasNext()) {
				Vertex referencedVertex = iterator.next();
				return reifyVertex(referencedVertex);
			} else {
				return null;
			}
		} else {
			Integer size = getSize(vertex, eReference);
			if (index < 0 || index >= size) {
			    NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Invalid get index " + index);
				throw new IndexOutOfBoundsException("Invalid get index " + index);
			} else {
				Iterator<Vertex> iterator = vertex.query().labels(eReference.getName()).direction(Direction.OUT).has(POSITION, index).vertices().iterator();
				if (iterator.hasNext()) {
					Vertex referencedVertex = iterator.next();
					return reifyVertex(referencedVertex);
				} else {
					return null;
				}
			}
		}
	}

	@Override
	public Object set(InternalEObject object, EStructuralFeature feature, int index, Object value) {
		Object returnValue = null;
		if (value == null) {
			returnValue = get(object, feature, index);
			clear(object, feature);
		} else {
			if (feature instanceof EAttribute) {
				returnValue = set(object, (EAttribute) feature, index, value);
			} else if (feature instanceof EReference) {
				returnValue = set(object, (EReference) feature, index, (EObject) value);
			} else {
				throw new IllegalArgumentException(feature.toString());
			}
		}
		return returnValue;
	}

	protected Object set(InternalEObject object, EAttribute eAttribute, int index, Object value) {
		Object returnValue = null;
		Vertex vertex = graph.getOrCreateVertex(object);
		if (!eAttribute.isMany()) {
			Object property = vertex.getProperty(eAttribute.getName());
			returnValue = parseProperty(eAttribute, property);
			vertex.setProperty(eAttribute.getName(), serializeToProperty(eAttribute, value));
		} else {
			Integer size = getSize(vertex, eAttribute);
			if (index < 0 || index >= size) {
			    NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Invalid set index " + index);
				throw new IndexOutOfBoundsException("Invalid set index " + index);
			} else {
				String propertyName = eAttribute.getName() + SEPARATOR + index;
				returnValue = vertex.getProperty(propertyName);
				vertex.setProperty(propertyName, serializeToProperty(eAttribute, value));
			}
		}
		return returnValue;
	}

	protected Object set(InternalEObject object, EReference eReference, int index, EObject value) {
		Object returnValue = null;
		Vertex vertex = graph.getOrCreateVertex(object);
		Vertex newReferencedVertex = graph.getOrCreateVertex(value);

		// Update the containment reference if needed
		if (eReference.isContainment()) {
			updateContainment(eReference, vertex, newReferencedVertex);
		}
		
		if (!eReference.isMany()) {
			Iterator<Edge> iterator = vertex.getEdges(Direction.OUT, eReference.getName()).iterator();
			if (iterator.hasNext()) {
				Edge edge = iterator.next();
				Vertex referencedVertex = edge.getVertex(Direction.IN);
				returnValue = reifyVertex(referencedVertex);
				edge.remove();
			}
			vertex.addEdge(eReference.getName(), newReferencedVertex);
		} else {
			Integer size = getSize(vertex, eReference);
			if (index < 0 || index >= size) {
			    NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Invalid set index " + index);
				throw new IndexOutOfBoundsException("Invalid set index " + index);
			} else {
				Iterator<Edge> iterator = vertex.query().labels(eReference.getName()).direction(Direction.OUT).has(POSITION, index).edges().iterator();
				if (iterator.hasNext()) {
					Edge edge = iterator.next();
					Vertex referencedVertex = edge.getVertex(Direction.IN);
					returnValue = reifyVertex(referencedVertex);
					edge.remove();
				}
				Edge edge = vertex.addEdge(eReference.getName(), newReferencedVertex);
				edge.setProperty(POSITION, index);
			}
		}
		return returnValue;
	}

	@Override
	public boolean isSet(InternalEObject object, EStructuralFeature feature) {
		if (feature instanceof EAttribute) {
			return isSet(object, (EAttribute) feature);
		} else if (feature instanceof EReference) {
			return isSet(object, (EReference) feature);
		} else {
			throw new IllegalArgumentException(feature.toString());
		}
	}

	protected boolean isSet(InternalEObject object, EAttribute eAttribute) {
		Vertex vertex = graph.getVertex(object);
		if (vertex != null) {
			if (!eAttribute.isMany()) {
				return null != vertex.getProperty(eAttribute.getName());
			} else {
				return null != vertex.getProperty(eAttribute.getName() + SEPARATOR + SIZE_LITERAL);
			}
		} else {
			return false;
		}
	}

	protected boolean isSet(InternalEObject object, EReference eReference) {
		Vertex vertex = graph.getVertex(object);
		if (vertex != null) {
			Iterable<Vertex> vertices = vertex.getVertices(Direction.OUT, eReference.getName());
			return vertices.iterator().hasNext();
		} else {
			return false;
		}
	}

	@Override
	public void unset(InternalEObject object, EStructuralFeature feature) {
		if (feature instanceof EAttribute) {
			unset(object, (EAttribute) feature);
		} else if (feature instanceof EReference) {
			unset(object, (EReference) feature);
		} else {
			throw new IllegalArgumentException(feature.toString());
		}
	}

	protected void unset(InternalEObject object, EAttribute eAttribute) {
		Vertex vertex = graph.getVertex(object);
		if (!eAttribute.isMany()) {
			vertex.removeProperty(eAttribute.getName());
		} else {
			Integer size = vertex.getProperty(eAttribute.getName() + SEPARATOR + SIZE_LITERAL);
			for (int i = 0; i < size; i++) {
				vertex.removeProperty(eAttribute.getName() + SEPARATOR + i);
			}
			vertex.removeProperty(eAttribute.getName() + SEPARATOR + SIZE_LITERAL);
		}
	}

	protected void unset(InternalEObject object, EReference eReference) {
		Vertex vertex = graph.getVertex(object);
		if (!eReference.isMany()) {
			Iterator<Edge> iterator = vertex.getEdges(Direction.OUT, eReference.getName()).iterator();
			if (iterator.hasNext()) {
				iterator.next().remove();
			}
		} else {
			Iterator<Edge> iterator = vertex.query().labels(eReference.getName()).direction(Direction.OUT).edges().iterator();
			while (iterator.hasNext()) {
				iterator.next().remove();
			}
			vertex.removeProperty(eReference.getName() + SEPARATOR + SIZE_LITERAL);
		}
	}

	@Override
	public boolean isEmpty(InternalEObject object, EStructuralFeature feature) {
		return size(object, feature) == 0;
	}

	@Override
	public int size(InternalEObject object, EStructuralFeature feature) {
		Vertex vertex = graph.getVertex(object);
		if (vertex != null) {
			return getSize(vertex, feature);
		} else {
			return 0;
		}
	}

	protected static Integer getSize(Vertex vertex, EStructuralFeature feature) {
		Integer size = vertex.getProperty(feature.getName() + SEPARATOR + SIZE_LITERAL);
		return size != null ? size : 0;
	}

	protected static void setSize(Vertex vertex, EStructuralFeature feature, int size) {
		vertex.setProperty(feature.getName() + SEPARATOR + SIZE_LITERAL, size);
	}

	@Override
	public boolean contains(InternalEObject object, EStructuralFeature feature, Object value) {
		if (value == null) {
			return false;
		} else {
		    Vertex v = graph.getOrCreateVertex(object);
		    InternalPersistentEObject eValue = NeoEObjectAdapterFactoryImpl.getAdapter(value, InternalPersistentEObject.class);
		    if(feature instanceof EReference) {
		        for(Vertex vOut : v.getVertices(Direction.OUT, feature.getName())) { 
		            if(vOut.getId().equals(eValue.id().toString())) {
		                return true;
		            }
		        }
		        return false;
		    }
		    else {
		        // feature is an EAttribute
		        return ArrayUtils.contains(toArray(object, feature), value);
		    }
		}
	}

	@Override
	public int indexOf(InternalEObject object, EStructuralFeature feature, Object value) {
	    if(feature instanceof EAttribute) {
	        return ArrayUtils.indexOf(toArray(object, feature), value);
	    }
	    else if(feature instanceof EReference) {
	        if(value == null) {
	            return ArrayUtils.INDEX_NOT_FOUND;
	        }
	        Vertex inVertex = graph.getVertex(object);
	        Vertex outVertex = graph.getVertex((EObject)value);
	        Iterator<Edge> iterator = outVertex.getEdges(Direction.IN, feature.getName()).iterator();
	        while(iterator.hasNext()) {
	            Edge e = iterator.next();
	            if(e.getVertex(Direction.OUT).equals(inVertex)) {
	                return e.getProperty(POSITION);
	            }
	        }
	        return ArrayUtils.INDEX_NOT_FOUND;
	    }
	    else {
	        throw new IllegalArgumentException(feature.toString());
	    }
	}

	@Override
	public int lastIndexOf(InternalEObject object, EStructuralFeature feature, Object value) {
	    if(feature instanceof EAttribute) {
	        return ArrayUtils.lastIndexOf(toArray(object, feature), value);
	    }
	    else if(feature instanceof EReference) {
	        if(value == null) {
	            return ArrayUtils.INDEX_NOT_FOUND;
	        }
	        Vertex inVertex = graph.getVertex(object);
	        Vertex outVertex = graph.getVertex((EObject)value);
	        Iterator<Edge> iterator = outVertex.getEdges(Direction.IN, feature.getName()).iterator();
	        Edge lastPositionEdge = null;
	        while(iterator.hasNext()) {
	            Edge e = iterator.next();
	            if(e.getVertex(Direction.OUT).equals(inVertex)) {
	                if(lastPositionEdge == null || ((int)e.getProperty(POSITION)) > (int)lastPositionEdge.getProperty(POSITION)) {
	                    lastPositionEdge = e;
	                }
	            }
	        }
	        return(lastPositionEdge == null ? ArrayUtils.INDEX_NOT_FOUND : (int)lastPositionEdge.getProperty(POSITION));
	    }
	    else {
	        throw new IllegalArgumentException(feature.toString());
	    }
	}

	@Override
	public void add(InternalEObject object, EStructuralFeature feature, int index, Object value) {
		if (feature instanceof EAttribute) {
			add(object, (EAttribute) feature, index, value);
		} else if (feature instanceof EReference) {
			add(object, (EReference) feature, index, (EObject) value);
		} else {
			throw new IllegalArgumentException(feature.toString());
		}
	}

	protected void add(InternalEObject object, EAttribute eAttribute, int index, Object value) {
		Vertex vertex = graph.getOrCreateVertex(object);
		Integer size = getSize(vertex, eAttribute);
		size++;
		setSize(vertex, eAttribute, size);
		if (index < 0 || index > size) {
		    NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Invalid add index " + index);
			throw new IndexOutOfBoundsException("Invalid add index " + index);
		} else {
			for (int i = size - 1; i > index; i--) {
				Object movingProperty = vertex.getProperty(eAttribute.getName() + SEPARATOR + (i - 1));
				vertex.setProperty(eAttribute.getName() + SEPARATOR + i, movingProperty);
			}
			vertex.setProperty(eAttribute.getName() + SEPARATOR + index, serializeToProperty(eAttribute, value));
		}
	}

	protected void add(InternalEObject object, EReference eReference, int index, EObject value) {
		Vertex vertex = graph.getOrCreateVertex(object);
		
		Vertex referencedVertex = graph.getOrCreateVertex(value);
		// Update the containment reference if needed
		if (eReference.isContainment()) {
			updateContainment(eReference, vertex, referencedVertex);
		}

		Integer size = getSize(vertex, eReference);
		int newSize = size + 1;
		if (index < 0 || index > newSize) {
	        NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Invalid add index " + index);
			throw new IndexOutOfBoundsException("Invalid add index " + index);
		} else {
		    if(index != size) {
		        // Avoid unnecessary database access
    			Iterator<Edge> iterator = vertex.query().labels(eReference.getName()).direction(Direction.OUT).interval(POSITION, index, newSize).edges()
    					.iterator();
    			while (iterator.hasNext()) {
    				Edge edge = iterator.next();
    				int position = edge.getProperty(POSITION);
    				edge.setProperty(POSITION, position + 1);
    			}
		    }
			Edge edge = vertex.addEdge(eReference.getName(), referencedVertex);
			edge.setProperty(POSITION, index);
		}
		setSize(vertex, eReference, newSize);
	}

	@Override
	public Object remove(InternalEObject object, EStructuralFeature feature, int index) {
		Object returnValue = null;
		if (feature instanceof EAttribute) {
			returnValue = remove(object, (EAttribute) feature, index);
		} else if (feature instanceof EReference) {
			returnValue = remove(object, (EReference) feature, index);
		} else {
			throw new IllegalArgumentException(feature.toString());
		}
		return returnValue;
	}

	protected Object remove(InternalEObject object, EAttribute eAttribute, int index) {
		Vertex vertex = graph.getVertex(object);
		Integer size = getSize(vertex, eAttribute);
		Object returnValue = null;
		if (index < 0 || index > size) {
		    NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Invalid remove index " + index);
			throw new IndexOutOfBoundsException("Invalid remove index " + index);
		} else {
			returnValue = parseProperty(eAttribute, vertex.getProperty(eAttribute.getName() + SEPARATOR + index));
			int newSize = size - 1;
			for (int i = newSize; i > index; i--) {
				Object movingProperty = vertex.getProperty(eAttribute.getName() + SEPARATOR + i);
				vertex.setProperty(eAttribute.getName() + SEPARATOR + (i - 1), movingProperty);
			}
			setSize(vertex, eAttribute, newSize);
		}
		return returnValue;
	}

	protected Object remove(InternalEObject object, EReference eReference, int index) {
		Vertex vertex = graph.getVertex(object);
		String referenceName = eReference.getName();
		Integer size = getSize(vertex, eReference);
		InternalEObject returnValue = null;
		if (index < 0 || index > size) {
		    NeoLogger.log(NeoLogger.SEVERITY_ERROR, "Invalid remove index " + index);
			throw new IndexOutOfBoundsException("Invalid remove index " + index);
		} else {
			Iterator<Edge> iterator = vertex.query().labels(referenceName).direction(Direction.OUT).interval(POSITION, index, size).edges().iterator();
			while (iterator.hasNext()) {
				Edge edge = iterator.next();
				int position = edge.getProperty(POSITION);
				if (position == index) {
					Vertex referencedVertex = edge.getVertex(Direction.IN);
					returnValue = reifyVertex(referencedVertex);
					edge.remove();
					if(eReference.isContainment()) {
						for (Edge conEdge : referencedVertex.getEdges(Direction.OUT, CONTAINER)) {
							conEdge.remove();
						}
					}
				} else {
					edge.setProperty(POSITION, position - 1);
				}
			}
		}
		setSize(vertex, eReference, size - 1); // Update size
		if(eReference.isContainment()) {
			returnValue.eBasicSetContainer(null, -1, null);
			((InternalPersistentEObject)returnValue).resource(null);
		}
		return returnValue;
	}

	@Override
	public Object move(InternalEObject object, EStructuralFeature feature, int targetIndex, int sourceIndex) {
		Object movedElement = remove(object, feature, sourceIndex);
		add(object, feature, targetIndex, movedElement);
		return movedElement;
	}

	@Override
	public void clear(InternalEObject object, EStructuralFeature feature) {
		if (feature instanceof EAttribute) {
			clear(object, (EAttribute) feature);
		} else if (feature instanceof EReference) {
			clear(object, (EReference) feature);
		} else {
			throw new IllegalArgumentException(feature.toString());
		}
	}

	protected void clear(InternalEObject object, EAttribute eAttribute) {
		Vertex vertex = graph.getVertex(object);
		Integer size = getSize(vertex, eAttribute);
		for (int i = 0; i < size; i++) {
			vertex.removeProperty(eAttribute.getName() + SEPARATOR + i);
		}
		setSize(vertex, eAttribute, 0);
	}

	protected void clear(InternalEObject object, EReference eReference) {
		Vertex vertex = graph.getOrCreateVertex(object);
		Iterator<Edge> iterator = vertex.query().labels(eReference.getName()).direction(Direction.OUT).edges().iterator();
		while (iterator.hasNext()) {
			iterator.next().remove();
		}
		setSize(vertex, eReference, 0);
	}

	@Override
	public Object[] toArray(InternalEObject object, EStructuralFeature feature) {
		int size = size(object, feature);
		Object[] result = new Object[size];
		for (int index = 0; index < size; index++) {
			result[index] = get(object, feature, index);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(InternalEObject object, EStructuralFeature feature, T[] array) {
		int size = size(object, feature);
		T[] result = null;
		if (array.length < size) {
			result = Arrays.copyOf(array, size);
		} else {
			result = array;
		}
		for (int index = 0; index < size; index++) {
			result[index] = (T) get(object, feature, index);
		}
		return result;
	}

	@Override
	public int hashCode(InternalEObject object, EStructuralFeature feature) {
		return toArray(object, feature).hashCode();
	}

	@Override
	public InternalEObject getContainer(InternalEObject object) {
		Vertex vertex = graph.getVertex(object);
		Iterator<Vertex> iterator = vertex.getVertices(Direction.OUT, CONTAINER).iterator();
		if (iterator.hasNext()) {
			return reifyVertex(iterator.next());
		}
		return null;
	}

	@Override
	public EStructuralFeature getContainingFeature(InternalEObject object) {
		Vertex vertex = graph.getVertex(object);
		Iterator<Edge> iterator = vertex.getEdges(Direction.OUT, CONTAINER).iterator();
		if (iterator.hasNext()) {
			Edge edge = iterator.next();
			String featureName = edge.getProperty(CONTAINING_FEATURE);
			Vertex containerVertex = edge.getVertex(Direction.IN);
	        if (featureName != null) {
                EObject container = reifyVertex(containerVertex);
                return container.eClass().getEStructuralFeature(featureName);
			}
		}
		return null;
	}

	@Override
	public EObject create(EClass eClass) {
		// This should not be called
		throw new UnsupportedOperationException();
	}

	protected static Object parseProperty(EAttribute eAttribute, Object property) {
		return property != null ? EcoreUtil.createFromString(eAttribute.getEAttributeType(), property.toString()) : null;
	}

	protected static Object serializeToProperty(EAttribute eAttribute, Object value) {
		return value != null ? EcoreUtil.convertToString(eAttribute.getEAttributeType(), value) : null;
	}
	
	protected static void updateContainment(EReference eReference, Vertex parentVertex, Vertex childVertex) {
		for (Edge edge : childVertex.getEdges(Direction.OUT, CONTAINER)) {
			edge.remove();
		}
		Edge edge = childVertex.addEdge(CONTAINER, parentVertex);
		edge.setProperty(CONTAINING_FEATURE, eReference.getName());
	}

	protected InternalEObject reifyVertex(Vertex vertex) {
		InternalPersistentEObject internalEObject = graph.reifyVertex(vertex);
		if(internalEObject.resource() != resource()) {
			Iterator<Edge> itE = vertex.getEdges(Direction.OUT, CONTAINER).iterator();
			if(!itE.hasNext()) {
				Iterator<Vertex> it = vertex.getVertices(Direction.IN,"eContents").iterator();
				if(it.hasNext()) {
//					System.out.println("EContents detected for " + internalEObject.eClass().getName());
					internalEObject.resource(resource());
				}
				else {
					// not part of the resource
				}
			}
			else {
				internalEObject.resource(resource());
			}
		}
		return internalEObject;
	}
	
	protected InternalEObject reifyVertex(Vertex vertex, EClass eClass) {
		InternalPersistentEObject internalEObject = graph.reifyVertex(vertex,eClass);
		if(internalEObject.resource() != resource()) {
			Iterator<Edge> itE = vertex.getEdges(Direction.OUT, CONTAINER).iterator();
			if(!itE.hasNext()) {
				Iterator<Vertex> it = vertex.getVertices(Direction.IN,"eContents").iterator();
				if(it.hasNext()) {
					internalEObject.resource(resource());
				}
				else {
					// not part of the resource
				}
			}
			else {
				internalEObject.resource(resource());
			}
		}
		return internalEObject;
	}
	
	@Override
	public EObject eObject(Id uriFragment) {
		Vertex vertex = graph.getVertex(uriFragment);
		return vertex != null ? reifyVertex(vertex) : null;
	}

	@Override
	public Resource.Internal resource() {
		return resource;
	}
	
	@Override
	public EList<EObject> getAllInstances(EClass eClass, boolean strict) {
		Map<EClass, Iterator<Vertex>> indexHits = graph.getAllInstances(eClass, strict);
		EList<EObject> instances = new BasicEList<EObject>();
		Set<EClass> mapKeys = indexHits.keySet();
		for(EClass metaClass : mapKeys) {
			Iterator<Vertex> instanceVertices = indexHits.get(metaClass);
			while(instanceVertices.hasNext()) {
				Vertex instanceVertex = instanceVertices.next();
				instances.add(reifyVertex(instanceVertex, metaClass));
			}
		}
		return instances;
	}
}
