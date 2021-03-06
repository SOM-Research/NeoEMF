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

package fr.inria.atlanmod.neoemf.io.processor;

import com.google.common.base.Stopwatch;

import fr.inria.atlanmod.neoemf.io.Handler;
import fr.inria.atlanmod.neoemf.util.logging.NeoLogger;

/**
 * An {@link Processor} that measures elapsed time between the start and the end of an I/O process.
 */
public class TimerProcessor extends AbstractProcessor {

    /**
     * The stopwatch.
     */
    private Stopwatch stopWatch;

    /**
     * Constructs a new {@code TimerProcessor} with the given {@code handler}.
     *
     * @param handler the handler to notify
     */
    public TimerProcessor(Handler handler) {
        super(handler);
    }

    @Override
    public void handleStartDocument() {
        NeoLogger.info("Document analysis in progress...");
        stopWatch = Stopwatch.createStarted();

        super.handleStartDocument();
    }

    @Override
    public void handleEndDocument() {
        NeoLogger.info("Document analysis done in {0}", stopWatch.stop());

        super.handleEndDocument();
    }
}