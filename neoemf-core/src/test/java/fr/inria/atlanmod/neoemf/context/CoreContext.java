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

package fr.inria.atlanmod.neoemf.context;

import fr.inria.atlanmod.neoemf.data.AbstractPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.data.PersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.data.store.DirectWriteStore;
import fr.inria.atlanmod.neoemf.resource.PersistentResource;
import fr.inria.atlanmod.neoemf.util.PersistenceURI;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;

/**
 * A specific {@link Context} for the core.
 */
public class CoreContext implements Context {

    /**
     * The name of this context.
     */
    public static final String NAME = "Core";

    /**
     * Constructs a new {@code CoreContext}.
     */
    protected CoreContext() {
    }

    /**
     * Returns the instance of this class.
     *
     * @return the instance of this class.
     */
    public static Context get() {
        return Holder.INSTANCE;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String uriScheme() {
        return "mock";
    }

    @Override
    public URI createURI(URI uri) {
        return PersistenceURI.createURI(uri);
    }

    @Override
    public URI createFileURI(File file) {
        return PersistenceURI.createFileURI(file, uriScheme());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This {@code Context} doesn't support the {@link PersistentResource} creation.
     *
     * @throws UnsupportedOperationException every time
     */
    @Override
    public PersistentResource createPersistentResource(EPackage ePackage, File file) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This {@code Context} doesn't support the {@link PersistentResource} creation.
     *
     * @throws UnsupportedOperationException every time
     */
    @Override
    public PersistentResource createTransientResource(EPackage ePackage, File file) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PersistenceBackendFactory persistenceBackendFactory() {
        return mock(AbstractPersistenceBackendFactory.class);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This {@code Context} doesn't have {@link DirectWriteStore} implementation.
     *
     * @throws UnsupportedOperationException every time
     */
    @Override
    public Class<? extends DirectWriteStore> directWriteClass() {
        throw new UnsupportedOperationException();
    }

    /**
     * The initialization-on-demand holder of the singleton of this class.
     */
    private static class Holder {

        /**
         * The instance of the outer class.
         */
        private static final Context INSTANCE = new CoreContext();
    }
}
