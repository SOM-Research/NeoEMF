/*
 * Copyright (c) 2013-2016 Atlanmod INRIA LINA Mines Nantes.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 */

package fr.inria.atlanmod.neoemf.benchmarks;

import fr.inria.atlanmod.neoemf.benchmarks.queries.Queries;
import fr.inria.atlanmod.neoemf.benchmarks.util.CommandLineUtil;
import fr.inria.atlanmod.neoemf.benchmarks.util.MessageUtil;
import fr.inria.atlanmod.neoemf.datastore.PersistenceBackendFactoryRegistry;
import fr.inria.atlanmod.neoemf.map.datastore.MapPersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.map.util.NeoMapURI;
import fr.inria.atlanmod.neoemf.resources.PersistentResourceFactory;
import fr.inria.atlanmod.neoemf.resources.impl.PersistentResourceImpl;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.gmt.modisco.java.Type;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static fr.inria.atlanmod.neoemf.benchmarks.util.CommandLineUtil.Key.EPACKAGE_CLASS;
import static fr.inria.atlanmod.neoemf.benchmarks.util.CommandLineUtil.Key.IN;

public class NeoEMFMapQueryOrphanNonPrimitiveTypes {

    private static final Logger LOG = LogManager.getLogger();

    public static void main(String[] args) {
        try {
            Map<String, String> cli = processCommandLineArgs(args);

            PersistenceBackendFactoryRegistry.register(NeoMapURI.NEO_MAP_SCHEME, MapPersistenceBackendFactory.getInstance());

            URI uri = NeoMapURI.createNeoMapURI(new File(cli.get(IN)));

            Class<?> inClazz = NeoEMFMapQueryOrphanNonPrimitiveTypes.class.getClassLoader().loadClass(cli.get(EPACKAGE_CLASS));
            inClazz.getMethod("init").invoke(null);

            ResourceSet resourceSet = new ResourceSetImpl();
            resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put(NeoMapURI.NEO_MAP_SCHEME, PersistentResourceFactory.getInstance());

            Resource resource = resourceSet.createResource(uri);

            Map<String, Object> loadOpts = new HashMap<>();
            resource.load(loadOpts);

            Queries.getOrphanNonPrimitivesTypes(resource).callWithTimeSpent();

            if (resource instanceof PersistentResourceImpl) {
                PersistentResourceImpl.shutdownWithoutUnload((PersistentResourceImpl) resource);
            }
            else {
                resource.unload();
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    private static Map<String, String> processCommandLineArgs(String... args) throws ParseException {
        Options options = new Options();

        options.addOption(Option.builder(IN)
                .argName("INPUT")
                .desc("Input NeoEMF resource directory")
                .hasArg()
                .required()
                .build());

        options.addOption(Option.builder(EPACKAGE_CLASS)
                .argName("CLASS")
                .desc("FQN of EPackage implementation class")
                .hasArg()
                .required()
                .build());

        return CommandLineUtil.getValues(options, args);
    }
}
