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

package fr.inria.atlanmod.neoemf.demo.importer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.JavaPackage;

import fr.inria.atlanmod.neoemf.data.PersistenceBackendFactoryRegistry;
import fr.inria.atlanmod.neoemf.data.hbase.HBasePersistenceBackendFactory;
import fr.inria.atlanmod.neoemf.data.hbase.option.HBaseOptionsBuilder;
import fr.inria.atlanmod.neoemf.data.hbase.util.HBaseURI;
import fr.inria.atlanmod.neoemf.resource.PersistentResource;
import fr.inria.atlanmod.neoemf.resource.PersistentResourceFactory;
import fr.inria.atlanmod.neoemf.util.logging.NeoLogger;

//EMF Compare related imports, uncomment to enable model comparison
//import java.util.List;
//import org.eclipse.emf.compare.Comparison;
//import org.eclipse.emf.compare.Diff;
//import org.eclipse.emf.compare.EMFCompare;
//import org.eclipse.emf.compare.match.IMatchEngine;
//import org.eclipse.emf.compare.match.impl.MatchEngineFactoryRegistryImpl;
//import org.eclipse.emf.compare.scope.DefaultComparisonScope;
//import org.eclipse.emf.compare.scope.IComparisonScope;
//import fr.inria.atlanmod.neoemf.util.emf.compare.LazyMatchEngineFactory;

/**
 * Imports an existing model stored in a XMI files into a HBase-based {@link PersistentResource}.
 */
public class HBaseImporter {

    public static void main(String[] args) throws IOException {
        JavaPackage.eINSTANCE.eClass();

        PersistenceBackendFactoryRegistry.register(HBaseURI.SCHEME, HBasePersistenceBackendFactory.getInstance());

        ResourceSet rSet = new ResourceSetImpl();
        rSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        rSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put(HBaseURI.SCHEME, PersistentResourceFactory.getInstance());

        try (PersistentResource persistentResource = (PersistentResource) rSet.createResource(HBaseURI.createHierarchicalURI("localhost", "2181", URI.createURI("sample.hbase")))) {
            Map<String, Object> options = HBaseOptionsBuilder.noOption();
            persistentResource.save(options);

            Instant start = Instant.now();

            Resource xmiResource = rSet.createResource(URI.createURI("models/sample.xmi"));
            xmiResource.load(Collections.emptyMap());

            persistentResource.getContents().addAll(xmiResource.getContents());
            persistentResource.save(options);

            Instant end = Instant.now();
            NeoLogger.info("HBase Model created in {0} seconds", Duration.between(start, end).getSeconds());
            
            /*
             * Checks that NeoEMF model contains the same elements as the input XMI.
             * This operation can take some time for large models because both input
             * and output models have to be entirely traversed.
             * This step is presented for the demonstration purpose and can be ignored
             * in real-world applications: NeoEMF ensures that created models from input 
             * XMI files contains all the input elements.
             */
//            IMatchEngine.Factory.Registry matchEngineRegistry = new MatchEngineFactoryRegistryImpl();
//            matchEngineRegistry.add(new LazyMatchEngineFactory());
//            IComparisonScope scope = new DefaultComparisonScope(xmiResource, persistentResource, null);
//            Comparison comparison = EMFCompare.builder().setMatchEngineFactoryRegistry(matchEngineRegistry).build().compare(scope);
//            
//            List<Diff> diffs = comparison.getDifferences();
//            if(diffs.size() > 0) {
//                NeoLogger.error("Created model has {0} diffs compared to the input XMI", diffs.size());
//                for(Diff diff : diffs) {
//                    NeoLogger.error("\t {0}", diff.toString());
//                }
//            }
//            else {
//                NeoLogger.info("Created model contains all the elements from the input XMI");
//            }
        }
    }
}

