package com.produsoft.workflow.config;

import java.net.InetAddress;
import java.net.ServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;

@Component
public class DynamicPortCustomizer implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    private static final Logger log = LoggerFactory.getLogger(DynamicPortCustomizer.class);

    @Value("${server.port:8080}")
    private int configuredPort;

    /**
     * Optional preferred fallback if the primary port is busy. If not set or also busy, the app will pick a random free port.
     */
    @Value("${app.server.fallback-port:0}")
    private int fallbackPort;

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        if (isPortAvailable(configuredPort)) {
            factory.setPort(configuredPort);
            return;
        }

        int candidate = fallbackPort > 0 && isPortAvailable(fallbackPort) ? fallbackPort : findEphemeralPort();
        factory.setPort(candidate);
        log.warn("Port {} is already in use. Starting server on available port {}", configuredPort, candidate);
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getByName("localhost"))) {
            socket.setReuseAddress(true);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private int findEphemeralPort() {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("localhost"))) {
            return socket.getLocalPort();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to locate an open port for the web server", ex);
        }
    }
}
