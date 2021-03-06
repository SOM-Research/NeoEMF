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
import fr.inria.atlanmod.neoemf.option.CommonOptionsBuilder;
import fr.inria.atlanmod.neoemf.resource.PersistentResource;
import fr.inria.atlanmod.neoemf.tests.models.mapSample.SampleModel;
import fr.inria.atlanmod.neoemf.tests.models.mapSample.SampleModelContentObject;

import org.eclipse.emf.common.util.EList;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CopyBackendContentTest extends AbstractBackendTest {

    private static final String MODEL_NAME = "Model", CONTENT1_NAME = "Content1", CONTENT2_NAME = "Content2";

    @Test
    @Category(Tags.TransientTests.class)
    public void testCopyBackend() throws IOException {
        PersistentResource resource = createTransientStore();
        createResourceContent(resource);

        resource.save(CommonOptionsBuilder.noOption());
        assertThat(resource.getContents()).isNotEmpty(); // "Map resource content is empty"
        assertThat(resource.getContents().get(0)).isInstanceOf(SampleModel.class); // "Top-level element is not a SampleModel"

        SampleModel sampleModel = (SampleModel) resource.getContents().get(0);
        assertThat(sampleModel.getName()).isEqualTo(MODEL_NAME); // "SampleModel has an invalid name attribute"

        EList<SampleModelContentObject> contentObjects = sampleModel.getContentObjects();
        assertThat(contentObjects).isNotEmpty(); // "SampleModel contentObjects collection is empty"
        assertThat(contentObjects).hasSize(2); // "SampleModel contentObjects collection has an invalid size"

        assertThat(contentObjects.get(0).getName()).isEqualTo(CONTENT1_NAME); // "First element in contentObjects collection has an invalid name"
        assertThat(contentObjects.get(1).getName()).isEqualTo(CONTENT2_NAME); // "Second element in contentObjects collection has an invalid name"

        assertThat(contentObjects.get(0).eContainer()).isSameAs(sampleModel); // "First element in contentObjects collection has an invalid container"
        assertThat(contentObjects.get(1).eContainer()).isSameAs(sampleModel); // "Second element in contentObjects collection has an invalid container"
    }

    private void createResourceContent(final PersistentResource resource) {
        SampleModel model = EFACTORY.createSampleModel();
        model.setName(MODEL_NAME);

        SampleModelContentObject content1 = EFACTORY.createSampleModelContentObject();
        content1.setName(CONTENT1_NAME);
        model.getContentObjects().add(content1);

        SampleModelContentObject content2 = EFACTORY.createSampleModelContentObject();
        content2.setName(CONTENT2_NAME);
        model.getContentObjects().add(content2);

        resource.getContents().add(model);
    }
}
