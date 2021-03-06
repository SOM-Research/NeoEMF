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

package fr.inria.atlanmod.neoemf.io.writer;

import fr.inria.atlanmod.neoemf.io.Handler;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link Handler} that writes data into a file.
 *
 * @note It correspond to the tail of the parsing process in case of an export.
 */
public interface Writer extends Handler {

    /**
     * Writes in a stream based on received notifications.
     *
     * @param stream the stream where to write
     *
     * @throws IOException if an error occurred during the I/O process
     */
    void write(OutputStream stream) throws IOException;
}
