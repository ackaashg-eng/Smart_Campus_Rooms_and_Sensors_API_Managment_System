package com.smartcampus;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS Application bootstrap for Tomcat deployment.
 *
 * With Tomcat, this class is loaded by the Jersey ServletContainer
 * configured in web.xml — there is no Main.java needed.
 *
 * Tomcat reads web.xml on startup, creates the Jersey servlet,
 * and passes control to this class to configure the application.
 *
 * The URL mapping (/api/v1/*) is defined in web.xml, so
 * @ApplicationPath is not used here — web.xml handles routing.
 *
 * Lifecycle Note:
 * JAX-RS resource classes are Request-Scoped by default — a new instance
 * is created per HTTP request. DataStore singleton with ConcurrentHashMap
 * is used to safely share in-memory data across all requests.
 */
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Auto-scan all sub-packages for resources, filters, exception mappers
        packages("com.smartcampus");
        // Enable Jackson for automatic POJO <-> JSON serialisation
        register(JacksonFeature.class);
    }
}
