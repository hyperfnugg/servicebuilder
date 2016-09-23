package no.obos.util.servicebuilder;

import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.jersey.config.JerseyJaxrsConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Legger til swagger-servlet. Standard path er tjeneste/versjon/api/swagger.json
 */
@AllArgsConstructor
public class SwaggerAddon extends ServiceAddonEmptyDefaults {

    public static final String DEFAULT_PATH_SPEC = "/swagger";
    public static final String DEFAULT_API_VERSION = "N/A";
    public static final String CONFIG_KEY_API_BASEURL = "api.baseurl";

    public final Configuration configuration;

    public static Configuration.ConfigurationBuilder defaultConfiguration() {
        return Configuration.builder()
                .pathSpec(DEFAULT_PATH_SPEC)
                .apiVersion(DEFAULT_API_VERSION);
    }

    public static void configFromProperties(PropertyProvider properties, Configuration.ConfigurationBuilder configBuilder) {
        properties.failIfNotPresent(CONFIG_KEY_API_BASEURL);
        configBuilder.apiBasePath(properties.get(CONFIG_KEY_API_BASEURL));
        String serviceVersion = properties.get(ServiceBuilder.CONFIG_KEY_SERVICE_VERSION);
        if (serviceVersion != null) {
            configBuilder.apiVersion(serviceVersion);
        }
    }

    @Override
    public void addToJerseyConfig(JerseyConfig jerseyConfig) {
        jerseyConfig.addRegistations(registrator -> registrator
                .register(ApiListingResource.class)
                .register(SwaggerSerializers.class)
        );
    }

    @Override
    public void addToJettyServer(JettyServer jettyServer) {
        ServletHolder apiDocServletHolder = new ServletHolder(new JerseyJaxrsConfig());
        apiDocServletHolder.setInitParameter("api.version", configuration.apiVersion);
        //Remove leading / as swagger adds its own
        String apiBasePath =
                "//".equals(configuration.apiBasePath.substring(0, 1))
                        ? configuration.apiBasePath.substring(1)
                        : configuration.apiBasePath;
        apiDocServletHolder.setInitParameter("swagger.api.basepath", apiBasePath);
        apiDocServletHolder.setInitOrder(2); //NOSONAR
        jettyServer.getServletContext().addServlet(apiDocServletHolder, configuration.pathSpec);
    }

    @Builder
    @AllArgsConstructor
    public static class Configuration {

        public final String apiBasePath;
        public final String pathSpec;
        public final String apiVersion;
    }


    //Det etterfølgende er generisk kode som er vanskelig å flytte ut i egne klasser pga generics. Kopier mellom addons.
    @AllArgsConstructor
    public static class AddonBuilder implements ServiceAddonConfig<SwaggerAddon> {
        Configurator options;
        Configuration.ConfigurationBuilder configBuilder;

        @Override
        public void addProperties(PropertyProvider properties) {
            configFromProperties(properties, configBuilder);
        }

        @Override
        public void addContext(ServiceBuilder serviceBuilder) {
            configFromContext(serviceBuilder, configBuilder);
        }

        @Override
        public SwaggerAddon init() {
            configBuilder = options.apply(configBuilder);
            return new SwaggerAddon(configBuilder.build());
        }
    }

    public static AddonBuilder configure(Configurator options) {
        return new AddonBuilder(options, defaultConfiguration());
    }

    public static AddonBuilder defaults() {
        return new AddonBuilder(cfg -> cfg, defaultConfiguration());
    }

    public interface Configurator {
        Configuration.ConfigurationBuilder apply(Configuration.ConfigurationBuilder configBuilder);
    }
}
