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

package fr.inria.atlanmod.neoemf.tests;

import fr.inria.atlanmod.neoemf.context.Tags;
import fr.inria.atlanmod.neoemf.resource.PersistentResource;
import fr.inria.atlanmod.neoemf.tests.models.mapSample.AbstractPackContentComment;
import fr.inria.atlanmod.neoemf.tests.models.mapSample.Pack;
import fr.inria.atlanmod.neoemf.tests.models.mapSample.PackContent;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that adding a transient containment sub-tree to an existing {@link PersistentResource} add all its elements to
 * the resource.
 */
public class ContainmentTest extends AbstractBackendTest {

    @Test
    @Category(Tags.PersistentTests.class)
    public void testAddContainmentSubtree() {
        PersistentResource resource = createPersistentStore();

        Pack p1 = EFACTORY.createPack();
        p1.setName("p1");

        resource.getContents().add(p1);

        Pack p2 = EFACTORY.createPack();
        p2.setName("p2");
        PackContent pc1 = EFACTORY.createPackContent();

        Pack p3 = EFACTORY.createPack();
        p3.setName("p3");
        p2.getPacks().add(p3);
        pc1.setName("pc1");
        p3.getOwnedContents().add(pc1);

        p1.getPacks().add(p2);

        assertThat(p1.eStore()).isInstanceOf(context().directWriteClass());
        assertThat(p2.eStore()).isInstanceOf(context().directWriteClass());
        assertThat(p3.eStore()).isInstanceOf(context().directWriteClass());
        assertThat(pc1.eStore()).isInstanceOf(context().directWriteClass());

        assertThat(p1.resource()).isSameAs(resource);
        assertThat(p2.resource()).isSameAs(resource);
        assertThat(p3.resource()).isSameAs(resource);
        assertThat(pc1.resource()).isSameAs(resource);
    }

    @Test
    @Category(Tags.PersistentTests.class)
    public void testSetContainmentAfterNonContainment() {
        PersistentResource resource = createPersistentStore();

        Pack p1 = EFACTORY.createPack();
        p1.setName("p1");

        resource.getContents().add(p1);

        Pack p2 = EFACTORY.createPack();
        p2.setName("p2");
        p1.getPacks().add(p2);
        PackContent pc1 = EFACTORY.createPackContent();
        pc1.setName("pc1");
        p2.getOwnedContents().add(pc1);

        AbstractPackContentComment com1 = EFACTORY.createAbstractPackContentComment();
        com1.setContent("My Content");

        // Add using the non-containment reference
        p2.getNonContainmentRefComments().add(com1);

        // Then add the element to the resource tree using the containment reference
        pc1.getContainmentNoOppositeRefComment().add(com1);

        assertThat(com1.eStore()).isInstanceOf(context().directWriteClass());
        assertThat(com1.resource()).isSameAs(resource);

        // Check that the element has a container (it cannot be in the resource if it does not)
        assertThat(com1.eContainer()).isSameAs(pc1);
        assertThat(com1.eInternalContainer()).isSameAs(pc1);

        // Check that the element is in the containment reference list of its parent
        assertThat(pc1.getContainmentNoOppositeRefComment()).contains(com1);

        // Check everything is accessible from the resource
        assertThat(resource.getAllContents()).hasSize(4);
    }
}
