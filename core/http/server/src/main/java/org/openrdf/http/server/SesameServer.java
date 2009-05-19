/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007-2009.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.http.server;

import static org.restlet.data.Status.CLIENT_ERROR_FORBIDDEN;
import static org.restlet.data.Status.SUCCESS_ACCEPTED;

import java.io.File;

import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Form;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;

/**
 * A Sesame server that can be started and stopped programmatically.
 */
public class SesameServer {

	/**
	 * The default server port (<tt>8080</tt>).
	 */
	public static final int DEFAULT_PORT = 8080;

	public static final String SHUTDOWN_PATH = "stop";

	public static final String SHUTDOWN_KEY_PARAM = "key";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final File dataDir;

	private final int port;

	private Component component;

	private LocalRepositoryManager manager;

	private String shutDownKey;

	/**
	 * Creates a new Sesame server that will listen to {@link #DEFAULT_PORT the
	 * default port}.
	 * 
	 * @param dataDir
	 *        The directory containing the server's configuration data,
	 *        repository data, etc.
	 */
	public SesameServer(File dataDir) {
		this(dataDir, DEFAULT_PORT);
	}

	/**
	 * Creates a new Sesame server.
	 * 
	 * @param dataDir
	 *        The directory containing the server's configuration data,
	 *        repository data, etc.
	 * @param port
	 *        The server port.
	 */
	public SesameServer(File dataDir, int port) {
		this.dataDir = dataDir;
		this.port = port;
	}

	/**
	 * Gets the server's data directory.
	 */
	public File getDataDir() {
		return dataDir;
	}

	/**
	 * Gets the server's port number.
	 */
	public int getPort() {
		return port;
	}

	public synchronized void setShutdownKey(String shutdownKey) {
		if (shutdownKey == null) {
			throw new IllegalArgumentException("shutdownKey must not be null");
		}

		this.shutDownKey = shutdownKey;
	}

	public synchronized String getShutdownKey() {
		return shutDownKey;
	}

	public RepositoryManager getRepositoryManager() {
		return manager;
	}

	/**
	 * Starts the server.
	 */
	public synchronized void start()
		throws Exception
	{
		manager = new LocalRepositoryManager(dataDir);
		manager.initialize();

		component = new Component();
		component.getServers().add(Protocol.HTTP, port);

		Context appContext = component.getContext().createChildContext();
		SesameApplication app = new SesameApplication(appContext, manager);
		component.getDefaultHost().attachDefault(app);
		if (getShutdownKey() != null) {
			component.getDefaultHost().attach("/" + SHUTDOWN_PATH, new StopRestlet(appContext));
		}

		try {
			component.start();
		}
		catch (Exception e) {
			stop();
			throw e;
		}
	}

	/**
	 * Stops the server.
	 */
	public synchronized void stop()
		throws Exception
	{
		try {
			component.stop();
		}
		finally {
			manager.shutDown();
		}
	}

	private void scheduleShutdown() {
		Thread t = new Thread("Server shutdown thread") {

			@Override
			public void run() {
				// give the server some time to reply to the shutdown request
				// FIXME: better to let the server shut down gracefully
				try {
					sleep(100L);
				}
				catch (InterruptedException ignore) {
				}

				try {
					SesameServer.this.stop();
				}
				catch (Exception e) {
					logger.error("Failed to stop server", e);
				}
			}
		};
		t.start();
	}

	public class StopRestlet extends Restlet {

		public StopRestlet(Context context) {
			super(context);
		}

		@Override
		public void handle(Request request, Response response) {
			if (request.getMethod().getName().equalsIgnoreCase("POST")) {
				handlePost(request, response);
			}
			else {
				response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
			}
		}

		private void handlePost(Request request, Response response) {
			Form params = request.getEntityAsForm();
			String key = params.getFirstValue(SHUTDOWN_KEY_PARAM);
			if (key != null && key.equals(getShutdownKey())) {
				response.setStatus(SUCCESS_ACCEPTED);
				logger.info("Server shutting down");
				scheduleShutdown();
			}
			else {
				response.setStatus(CLIENT_ERROR_FORBIDDEN, "invalid shutdown key");
			}
		}
	}
}
