/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.tools.ui;

import com.sun.jini.proxy.BasicProxyTrustVerifier;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.event.RemoteEvent;
import net.jini.core.event.RemoteEventListener;
import net.jini.core.event.UnknownEventException;
import net.jini.export.Exporter;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import org.rioproject.config.ExporterConfig;

import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple {@code RemoteEventListener} that sends recdived events to a {@code RemoteServiceEventListener}
 *
 * @author Dennis Reedy
 */
public class ChainedRemoteEventListener implements RemoteEventListener, ServerProxyTrust {
    private final RemoteEventListener eventListener;
    private final RemoteEventListener remoteEventListener;
    private final ExecutorService execService = Executors.newSingleThreadExecutor();
    private final BlockingQueue<RemoteEvent> eventQ = new LinkedBlockingQueue<RemoteEvent>();
    private static final Logger logger = Logger.getLogger(ChainedRemoteEventListener.class.getName());

    public ChainedRemoteEventListener(RemoteEventListener eventListener,
                                      Configuration configuration) throws ConfigurationException, ExportException {
        if (eventListener == null)
            throw new IllegalArgumentException("eventListener must not be null");
        this.eventListener = eventListener;
        final Exporter defaultExporter = new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
                                                               new BasicILFactory(),
                                                               false,
                                                               true);
        final Exporter exporter = ExporterConfig.getExporter(configuration,
                                                             "org.rioproject.event",
                                                             "eventConsumerExporter",
                                                             defaultExporter);
        remoteEventListener = (RemoteEventListener) exporter.export(this);
        execService.submit(new EventNotifier());
    }

    public RemoteEventListener getRemoteEventListener() {
        return remoteEventListener;
    }

    public void terminate() {
        execService.shutdownNow();
    }

    public void notify(RemoteEvent remoteEvent) throws UnknownEventException, RemoteException {
        eventQ.offer(remoteEvent);
    }

    @Override
    public TrustVerifier getProxyVerifier() throws RemoteException {
        return new BasicProxyTrustVerifier(remoteEventListener);
    }

    class EventNotifier implements Runnable {

        public void run() {
            while (true) {
                RemoteEvent event;
                try {
                    event = eventQ.take();
                } catch (InterruptedException e) {
                    logger.info("EventNotifier breaking out of main loop");
                    break;
                }
                try {
                    eventListener.notify(event);
                } catch (UnknownEventException e) {
                    logger.log(Level.WARNING, "Unknown event "+event.getClass().getName(), e);
                } catch (RemoteException e) {
                    logger.log(Level.WARNING, "Should not happen!", e);
                }
            }
        }
    }
}