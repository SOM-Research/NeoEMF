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

package fr.inria.atlanmod.neoemf.benchmarks.datastore.helper;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class Workspace {

    private static final Logger log = LogManager.getLogger();

    private static Path BASE_DIRECTORY;
    private static Path RESOURCES_DIRECTORY;
    private static Path STORES_DIRECTORY;
    private static Path TEMP_DIRECTORY;

    private Workspace() {
    }

    public static Path getBaseDirectory() {
        if (isNull(BASE_DIRECTORY)) {
            try {
                BASE_DIRECTORY = Files.createDirectories(
                        Paths.get(System.getProperty("user.home"))
                                .resolve(".neoemf")
                                .resolve("benchmarks"));
            }
            catch (IOException e) {
                log.warn(e);
            }
        }
        return BASE_DIRECTORY;
    }

    public static Path getResourcesDirectory() {
        if (isNull(RESOURCES_DIRECTORY)) {
            try {
                RESOURCES_DIRECTORY = Files.createDirectories(getBaseDirectory().resolve("resources"));
            }
            catch (IOException e) {
                log.warn(e);
            }
        }
        return RESOURCES_DIRECTORY;
    }

    public static Path getStoreDirectory() {
        if (isNull(STORES_DIRECTORY)) {
            try {
                STORES_DIRECTORY = Files.createDirectories(getBaseDirectory().resolve("stores"));
            }
            catch (IOException e) {
                log.warn(e);
            }
        }
        return STORES_DIRECTORY;
    }

    private static Path getTempDirectory() {
        if (isNull(TEMP_DIRECTORY)) {
            try {
                TEMP_DIRECTORY = Files.createTempDirectory("neoemf-benchmark");
            }
            catch (IOException e) {
                log.warn(e);
            }
        }
        return TEMP_DIRECTORY;
    }

    public static Path newTempDirectory() {
        Path tempDirectory = null;
        try {
            tempDirectory = Files.createTempDirectory(getTempDirectory(), "tmp");
        }
        catch (IOException e) {
            log.error(e);
        }
        return tempDirectory;
    }

    public static void cleanTempDirectory() {
        if (nonNull(TEMP_DIRECTORY)) {
            try {
                FileUtils.cleanDirectory(TEMP_DIRECTORY.toFile());
            }
            catch (IOException ignore) {
            }
        }
    }
}
