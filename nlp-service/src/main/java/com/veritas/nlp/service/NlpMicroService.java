package com.veritas.nlp.service;

import com.veritas.nlp.resources.ApiRoot;
import com.veritas.nlp.resources.NerResource;
import com.veritas.nlp.resources.ResourceExceptionMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.management.ManagementFactory;

public class NlpMicroService extends Application<NlpConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(NlpMicroService.class);
    private static final boolean EXECUTE_FILTERS_IN_ORDER_ADDED = true;
    private static String processName;
    private NlpServiceSettings settings;
    private Environment environment;
    private ShutdownListener shutdownListener;

    public static void main(String[] args) throws Exception {
        MDC.put("processName", getProcessName());
        NlpMicroService nlpMicroService = new NlpMicroService();
        nlpMicroService.run(args);
    }

    static String getProcessName() {
        if (StringUtils.isBlank(processName)) {
            String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("@.*", "");
            processName = getServiceName() + ":" + pid;
        }
        return processName;
    }

    @Override
    public String getName() {
        return getServiceName();
    }

    @Override
    public void initialize(Bootstrap<NlpConfiguration> bootstrap) {
        bootstrap.addBundle(new NlpSwaggerBundle());

        // Support environment variables in configuration, e.g. ${ProgramData} in a path to a log file
        // Also support loading yml from the class path (in addition to the file system)
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        new FileOrClassPathConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(NlpConfiguration configuration, Environment environment) throws Exception {
        this.settings = configuration.getNlpServiceSettings();
        this.environment = environment;
        checkJavaEnvironment();
        setupLifecycleHooks();
        setupLoggingFilter();
        setupRegistrations();
        startShutdownListener();
    }

    public void stop() {
        // Let DropWizard's (well Jetty's really) shutdown hook take care of
        // graceful shutdown so everything is shutdown in the correct order.
        LOG.info("Stopping");
        systemExit(0);
    }

    @SuppressFBWarnings(value="DM_EXIT", justification = "Shutdown hooks in place")
    private static void systemExit(int exitCode) {
        System.exit(exitCode);
    }

    private void checkJavaEnvironment() {
        double javaVersion = Double.parseDouble(System.getProperty("java.specification.version"));
        if (javaVersion < 1.8) {
            LOG.error("Cannot start due to incorrect Java version. Minimum version is 1.8. Actual version is [{}]", javaVersion);
            System.out.println("Cannot start due to unsupported Java version. Version 1.8 or later required.");
            systemExit(-1);
        }
    }

    private void setupRegistrations() throws Exception {
        NerResource nerResource = new NerResource(settings);
        environment.jersey().register(new ResourceExceptionMapper());
        environment.jersey().register(nerResource);
        environment.jersey().register(new ApiRoot());
        environment.jersey().register(MultiPartFeature.class);
        environment.healthChecks().register("NLP health check", new NlpHealthCheck(nerResource));
    }

    private void setupLoggingFilter() {
        environment.servlets().addFilter("LoggingFilter", LoggingFilter.class)
            .addMappingForUrlPatterns(null, EXECUTE_FILTERS_IN_ORDER_ADDED, "/*");
    }

    private void setupLifecycleHooks() {
        environment.lifecycle().manage(new Managed(){
            @Override public void start() throws Exception {
                int port = getPort();
                int adminPort = getAdminPort();
                LOG.info("Started listening on port {} and admin port {}", port, adminPort);
                // Write PORT:<port> and ADMINPORT:<adminPort> lines to System.out so that
                // the launching process can determine the actual ports being listened on.
                System.out.printf("%nPORT:%d%nADMINPORT:%d%n%n", port, adminPort);
            }
            @Override public void stop() throws Exception {
                LOG.info("Stopped");
            }
        });
    }

    /**
     * @return The actual port the connector is listening on
     * (this is the 1st NetworkConnector), or 0 if not open.
     */
    private int getPort() {
        Connector[] connectors = environment.getApplicationContext().getServer().getConnectors();
        if (connectors != null) {
            for (Connector connector : connectors) {
                if (connector instanceof NetworkConnector) {
                    return ((NetworkConnector)connector).getLocalPort();
                }
            }
        }
        return 0;
    }

    /**
     * @return The actual admin port the connector is listening on
     * (this is the 2nd NetworkConnector), or 0 if not open.
     */
    private int getAdminPort() {
        Connector[] connectors = environment.getAdminContext().getServer().getConnectors();
        if (connectors != null) {
            int i = 0;
            for (Connector connector : connectors) {
                if (connector instanceof NetworkConnector) {
                    if (++i == 2) {
                        return ((NetworkConnector)connector).getLocalPort();
                    }
                }
            }
        }
        return 0;
    }

    private static String getServiceName() {
        return "nlp";
    }

    private void startShutdownListener() {
        if (settings.isShutdownListenerEnabled()) {
            shutdownListener = new ShutdownListener(this::stop);
            shutdownListener.start();
        }
    }
}
