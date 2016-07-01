/*
 * Copyright (c) 2013 Atlanmod INRIA LINA Mines Nantes.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Atlanmod INRIA LINA Mines Nantes - initial API and implementation
 */

package fr.inria.atlanmod.neoemf.io.input.xmi;

import fr.inria.atlanmod.neoemf.io.internal.InternalHandler;
import fr.inria.atlanmod.neoemf.io.internal.impl.DefaultInternalHandler;
import fr.inria.atlanmod.neoemf.io.internal.impl.EcoreDelegatedInternalHandler;
import fr.inria.atlanmod.neoemf.io.internal.impl.XPathDelegatedInternalHandler;
import fr.inria.atlanmod.neoemf.logger.NeoLogger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.SAXParserFactory;

/**
 * A XMI {@link fr.inria.atlanmod.neoemf.io.input.Reader reader} that uses stream for reading.
 * <p/>
 * It uses a SAX implementation of XML parsing.
 */
public class XmiStreamReader extends AbstractXmiReader {

    @Override
    public InternalHandler newDefaultHandler() {
        InternalHandler defaultHandler;

        defaultHandler = new DefaultInternalHandler();
        defaultHandler = new XPathDelegatedInternalHandler(defaultHandler);
        defaultHandler = new EcoreDelegatedInternalHandler(defaultHandler);

        addHandler(defaultHandler);
        return defaultHandler;
    }

    public void read(File file) throws Exception {
        if (!hasHandler()) {
            NeoLogger.error("This notifier hasn't any handler.");
            return;
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);

        InputStream stream = new FileInputStream(file);

        Timer logProgressTimer = new Timer(true);

        try {
            // Log progress timer
            logProgressTimer.schedule(new LogProgressTimer(stream, file.length()), 10000, 30000);

            factory.newSAXParser().parse(stream, new XmiSaxHandler());
        } catch (SAXException e) {
            throw new Exception(e);
        } finally {
            logProgressTimer.cancel();
        }
    }

    private class XmiSaxHandler extends DefaultHandler {

        @Override
        public void startDocument() throws SAXException {
            try {
                XmiStreamReader.this.notifyStartDocument();
            }
            catch (Exception e) {
                NeoLogger.error(e);
                throw new SAXException(e);
            }

            logProgress(0);
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            processNamespace(prefix, uri);
        }

        @Override
        public void startElement(String uri, String name, String qName, Attributes attributes) throws SAXException
        {
            try {
                processElement(uri, name, attributes);
            }
            catch (Exception e) {
                NeoLogger.error(e);
                throw new SAXException(e);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String characters = String.valueOf(ch, start, length).trim();
            try {
                if (!characters.isEmpty()) {
                    processCharacters(characters);
                }
            }
            catch (Exception e) {
                NeoLogger.error(e);
                throw new SAXException(e);
            }
        }

        @Override
        public void endElement(String uri, String name, String qName) throws SAXException {
            try {
                XmiStreamReader.this.notifyEndElement();
            }
            catch (Exception e) {
                NeoLogger.error(e);
                throw new SAXException(e);
            }
        }

        @Override
        public void endDocument() throws SAXException {
            logProgress(100);

            try {
                XmiStreamReader.this.notifyEndDocument();
            }
            catch (Exception e) {
                NeoLogger.error(e);
                throw new SAXException(e);
            }
        }
    }

    private static class LogProgressTimer extends TimerTask {

        private final InputStream stream;
        private final long total;

        private LogProgressTimer(InputStream stream, long total) {
            this.stream = stream;
            this.total = total;
        }

        @Override
        public void run() {
            try {
                logProgress((double) (total - stream.available()) / (double) total * 100d);
            }
            catch (Exception e) {
                // No nothing
            }
        }
    }

    private static void logProgress(double percent) {
        NeoLogger.debug("Progress : {0}", String.format("%5s", String.format("%,.0f %%", percent)));
    }
}
