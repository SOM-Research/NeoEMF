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

package fr.inria.atlanmod.neoemf.data.blueprints.context;

import fr.inria.atlanmod.neoemf.context.ResourceBuilder;
import fr.inria.atlanmod.neoemf.data.blueprints.AbstractBlueprintsResourceBuilder;
import fr.inria.atlanmod.neoemf.data.blueprints.option.BlueprintsResourceOptions;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * A specific {@link ResourceBuilder} for the Blueprints implementation.
 */
public class BlueprintsResourceBuilder extends AbstractBlueprintsResourceBuilder<BlueprintsResourceBuilder> {

    /**
     * Constructs a new {@code BlueprintsResourceBuilder} with the given {@code ePackage}.
     *
     * @param ePackage the {@link EPackage} associated to the built {@link Resource}
     *
     * @see EPackage.Registry
     */
    public BlueprintsResourceBuilder(EPackage ePackage) {
        super(ePackage);
    }

    @Override
    protected void registerFactory() {
        resourceOptions.put(BlueprintsResourceOptions.GRAPH_TYPE, BlueprintsResourceOptions.GRAPH_TYPE_DEFAULT);
    }
}
