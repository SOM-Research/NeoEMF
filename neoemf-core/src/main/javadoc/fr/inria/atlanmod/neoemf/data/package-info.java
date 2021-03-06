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

/**
 * Provides generic classes representing data management operations.
 * <p>
 * This package defines a set of abstract classes defining the low-level backend operations and the model-to-backend
 * mapping. They are extended in backend-specific packages with a concrete implementation tailored to the corresponding
 * backend.
 * <p>
 * Every backend supported by NeoEMF implements the {@link fr.inria.atlanmod.neoemf.data.PersistenceBackend} interface,
 * that defines a set of primitives that allows the core component to access a database and serialize model elements.
 * These specific implementations have to be registered in the {@link fr.inria.atlanmod.neoemf.data.PersistenceBackendFactoryRegistry}
 * to allow their instanciation by the framework.
 */

package fr.inria.atlanmod.neoemf.data;