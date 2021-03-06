package jante;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import jante.client.ClientGenerator;
import jante.client.StubGenerator;
import jante.client.TargetGenerator;
import jante.config.PropertyMap;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.function.Function.identity;
import static jante.client.ClientGenerator.clientGenerator;
import static jante.client.StubGenerator.stubGenerator;
import static jante.client.TargetGenerator.targetGenerator;


@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TestServiceRunner implements TestServiceRunnerBase {
    @Getter
    @Wither(AccessLevel.PRIVATE)
    private final ServiceConfig serviceConfig;
    @Wither(AccessLevel.PRIVATE)
    public final Function<ClientGenerator, ClientGenerator> clientConfigurator;
    @Wither(AccessLevel.PRIVATE)
    public final Function<StubGenerator, StubGenerator> stubConfigurator;
    @Wither(AccessLevel.PRIVATE)
    public final Function<TargetGenerator, TargetGenerator> targetConfigurator;
    @Getter
    @Wither(AccessLevel.PRIVATE)
    public final Runtime runtime;
    @Wither(AccessLevel.PRIVATE)
    public final PropertyMap properties;

    public static TestServiceRunner testServiceRunner(ServiceConfig serviceConfig) {
        return new TestServiceRunner(serviceConfig, identity(), identity(), identity(), null, PropertyMap.propertyMap);
    }


    @AllArgsConstructor
    public static class Runtime implements TestRuntime {
        @Getter
        public final ServiceConfig.Runtime configRuntime;
        public final JerseyConfig jerseyConfig;
        public final TestContainer testContainer;
        public final ClientConfig clientConfig;
        public final URI uri;
        public final Client client;
        @Wither(AccessLevel.PRIVATE)
        public final Function<StubGenerator, StubGenerator> stubConfigurator;
        @Wither(AccessLevel.PRIVATE)
        public final Function<TargetGenerator, TargetGenerator> targetConfigurator;

        public void stop() {
            configRuntime.addons.addons.forEach(addon -> {
                try {
                    addon.cleanUp();
                } catch (RuntimeException ex) {
                    log.error("Exception during cleanup", ex);
                }
            });
            testContainer.stop();
        }

        public <T> T call(BiFunction<ClientConfig, URI, T> testfun) {
            return testfun.apply(clientConfig, uri);
        }

        @Override
        public <T> T call(Function<WebTarget, T> testfun) {
            TargetGenerator targetGenerator = targetConfigurator.apply(targetGenerator(client, uri));
            return testfun.apply(targetGenerator.generate());
        }

        @Override
        public <T, Y> T call(Class<Y> clazz, Function<Y, T> testfun) {
            StubGenerator stubGenerator = stubConfigurator.apply(stubGenerator(client, uri));
            return testfun.apply(stubGenerator.generateClient(clazz));
        }

        @Override
        public <Y> void callVoid(Class<Y> clazz, Consumer<Y> testfun) {
            StubGenerator stubGenerator = stubConfigurator.apply(stubGenerator(client, uri));
            testfun.accept(stubGenerator.generateClient(clazz));
        }

        @Override
        public void callVoid(Consumer<WebTarget> testfun) {
            TargetGenerator targetGenerator = targetConfigurator.apply(targetGenerator(client, uri));
            testfun.accept(targetGenerator.generate());
        }

        @Override
        public ResourceConfig getResourceConfig() {
            return jerseyConfig.getResourceConfig();
        }

        public Runtime stubConfigurator(Function<StubGenerator, StubGenerator> stubConfigurator) {
            return withStubConfigurator(stubConfigurator);
        }

        public Runtime targetConfigurator(Function<TargetGenerator, TargetGenerator> targetConfigurator) {
            return withTargetConfigurator(targetConfigurator);
        }
    }

    public TestServiceRunner start() {

        ServiceConfig.Runtime configRuntime = serviceConfig.applyProperties(properties);

        JerseyConfig jerseyConfig = new JerseyConfig(configRuntime.serviceDefinition, configRuntime.injections);

        DeploymentContext context = DeploymentContext.builder(jerseyConfig.getResourceConfig()).build();
        URI uri = UriBuilder.fromUri("http://localhost/").port(0).build();
        TestContainer testContainer = new InMemoryTestContainerFactory().create(uri, context);
        testContainer.start();
        ClientConfig clientConfig = testContainer.getClientConfig();
        ClientGenerator generator = clientConfigurator.apply(
                clientGenerator.serviceDefinition(configRuntime.serviceDefinition)
                        .clientConfigBase(clientConfig)
        );
        Client client = generator.generate();

        Runtime runtime = new Runtime(configRuntime, jerseyConfig, testContainer, clientConfig, uri, client, stubConfigurator, targetConfigurator);
        return withRuntime(runtime);
    }

    public TestServiceRunner withStartedRuntime() {
        return start();
    }

    public TestChain chain() {
        return new TestChain(this);
    }

    public <T> T oneShot(BiFunction<ClientConfig, URI, T> testfun) {
        Runtime runner = start().runtime;
        try {
            return testfun.apply(runner.clientConfig, runner.uri);
        } finally {
            runner.stop();
        }
    }

    public <T, Y> T oneShot(Class<Y> clazz, Function<Y, T> testfun) {
        Runtime runner = start().runtime;
        try {
            return runner.call(clazz, testfun);
        } finally {
            runner.stop();
        }
    }

    public <T> T oneShot(Function<WebTarget, T> testfun) {
        Runtime runner = start().runtime;
        try {
            return runner.call(testfun);
        } finally {
            runner.stop();
        }
    }

    public TestServiceRunner property(String key, String value) {
        return properties(this.properties.put(key, value));
    }

    public <Y> void oneShotVoid(Class<Y> clazz, Consumer<Y> testfun) {
        Runtime runner = start().runtime;
        try {
            runner.callVoid(clazz, testfun);
        } finally {
            runner.stop();
        }
    }


    public TestServiceRunner clientConfigurator(Function<ClientGenerator, ClientGenerator> clientConfigurator) {
        return withClientConfigurator(clientConfigurator);
    }

    public TestServiceRunner stubConfigurator(Function<StubGenerator, StubGenerator> stubConfigurator) {
        return withStubConfigurator(stubConfigurator);
    }

    public TestServiceRunner targetConfigurator(Function<TargetGenerator, TargetGenerator> targetConfigurator) {
        return withTargetConfigurator(targetConfigurator);
    }

    public TestServiceRunner properties(PropertyMap properties) {
        return withProperties(properties);
    }

    public TestServiceRunner serviceConfig(ServiceConfig serviceConfig) {
        return this.withServiceConfig(serviceConfig);
    }
}
