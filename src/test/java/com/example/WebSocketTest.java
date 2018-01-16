package com.example;

import org.eclipse.jetty.client.ConnectProxy;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.ProxyConfiguration;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class WebSocketTest {
    private static final String METHOD = "method";
    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private final WebSocketClient client = new WebSocketClient();

    private static final Properties properties = new Properties();

    @BeforeClass
    public static void configure() throws IOException {
//        System.setProperty("org.eclipse.jetty.LEVEL", "DEBUG");
        final URL resource = WebSocketTest.class.getResource("/proxy.properties");
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                properties.load(stream);
            }
            properties.values().removeIf(""::equals);
        }
    }

    @Before
    public void start() throws Exception {
        if (properties.containsKey(HOST) && properties.containsKey(PORT)) {
            configureProxy();
        } else {
            Log.getLog().info("Using direct connection");
        }
        client.start();
    }

    private void configureProxy() {
        final ProxyConfiguration.Proxy proxy = "CONNECT".equals(properties.getProperty(METHOD))
                ? new ConnectProxy(properties.getProperty(HOST), Integer.parseInt(properties.getProperty(PORT)))
                : new HttpProxy(properties.getProperty(HOST), Integer.parseInt(properties.getProperty(PORT)));
        Log.getLog().info("Using proxy " + proxy + " (" + proxy.getClass().getSimpleName() + ")");
        client.getHttpClient().getProxyConfiguration().getProxies().add(proxy);
        if (properties.containsKey(USERNAME) && properties.containsKey(PASSWORD)) {
            final BasicAuthentication authentication = new BasicAuthentication(proxy.getURI(),
                    Authentication.ANY_REALM, properties.getProperty(USERNAME), properties.getProperty(PASSWORD, ""));
            client.getHttpClient().getAuthenticationStore().addAuthentication(authentication);
        }
    }

    @After
    public void stop() throws Exception {
        client.stop();
    }

    @Test
    public void run() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        MySocket socket = new MySocket();
        Future<Session> future = client.connect(socket, URI.create("ws://echo.websocket.org"), new ClientUpgradeRequest());
        final Session session = future.get(5, TimeUnit.SECONDS);
        Thread.sleep(2_000);
        session.close();
        Thread.sleep(2_000);
        assertThat(socket.log, contains("{connected}", "TEST", "{closed}"));
    }
}
