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

import fr.inria.atlanmod.neoemf.context.Context;
import fr.inria.atlanmod.neoemf.context.ContextualTest;
import fr.inria.atlanmod.neoemf.data.blueprints.context.BlueprintsContext;

/**
 * A test-case that uses a {@link BlueprintsContext}.
 */
public interface BlueprintsTest extends ContextualTest {

    @Override
    default Context context() {
        return BlueprintsContext.get();
    }
}
