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

package fr.inria.atlanmod.neoemf.data.blueprints.neo4j.option;

import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

import fr.inria.atlanmod.neoemf.data.blueprints.option.BlueprintsResourceOptions;
import fr.inria.atlanmod.neoemf.option.PersistentResourceOptions;

/**
 * {@link PersistentResourceOptions} that hold Blueprints Neo4j related resource-level features, such as cache type,
 * usage of memory mapped files, or internal buffer sizes.
 */
public interface BlueprintsNeo4jResourceOptions extends BlueprintsResourceOptions {

    /**
     * The option value to define {@link Neo4jGraph} as the graph implementation to use.
     */
    String GRAPH_TYPE_NEO4J = "com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph";

    /**
     * The option key to define the cache type used by Neo4j.
     */
    String CACHE_TYPE = "blueprints.neo4j.conf.cache_type";

    /**
     * The option key to enable/disable the usage of memory mapped files.
     */
    String USE_MEMORY_MAPPED_BUFFERS = "blueprints.neo4j.conf.use_memory_mapped_buffers";

    /**
     * The option key to set the size of the buffer that contains string values.
     */
    String STRINGS_MAPPED_MEMORY = "blueprints.neo4j.conf.neostore.propertystore.db.strings.mapped_memory";

    /**
     * The option key to set the size of the buffer that contains arrays.
     */
    String ARRAYS_MAPPED_MEMORY = "blueprints.neo4j.conf.neostore.propertystore.db.arrays.mapped_memory";

    /**
     * The option key to set the size of the buffer that contains nodes.
     */
    String NODES_MAPPED_MEMORY = "blueprints.neo4j.conf.neostore.nodestore.db.mapped_memory";

    /**
     * The option key to set the size of the buffer that contains properties.
     */
    String PROPERTIES_MAPPED_MEMORY = "blueprints.neo4j.conf.neostore.propertystore.db.mapped_memory";

    /**
     * The option key to set the size of the buffer that contains relationships.
     */
    String RELATIONSHIPS_MAPPED_MEMORY = "blueprints.neo4j.conf.neostore.relationshipstore.db.mapped_memory";

    /**
     * Possible values for {@link #CACHE_TYPE}.
     * <p>
     * The cache type "hpc" is not available because the embedded Neo4j is the Community Edition.
     */
    enum CacheType {

        /**
         *
         */
        NONE("none"),

        /**
         *
         */
        SOFT("soft"),

        /**
         *
         */
        WEAK("weak"),

        /**
         *
         */
        STRONG("strong");

        /**
         * The value of the property.
         */
        private final String value;

        /**
         * Constructs a new {@code CacheType} with its {@code value}.
         *
         * @param value the value of the property
         */
        CacheType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
