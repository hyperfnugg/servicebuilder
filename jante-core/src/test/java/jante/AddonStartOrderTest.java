package jante;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import jante.model.ServiceDefinition;
import jante.model.Version;
import jante.model.Addon;
import jante.model.ServiceDefinition;
import jante.model.Version;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static jante.ServiceConfig.serviceConfig;
import static jante.config.PropertyMap.propertyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class AddonStartOrderTest {

    public static class MyServiceDefinition implements ServiceDefinition {
        @Override
        public String getName() {
            return "testService";
        }

        @Override
        public Version getVersion() {
            return new Version(1, 0, 0);
        }

        @Override
        public List<Class> getResources() {
            return Lists.newArrayList();
        }
    }


    static final MyServiceDefinition serviceDefinition = new MyServiceDefinition();

    @Test
    public void addons_are_started_in_config_order_when_no_order_specified() {
        //Given
        final List<Integer> startOrder = Lists.newArrayList();
        ServiceConfig config = serviceConfig(serviceDefinition)
                .addon(new Addon() {
                    @Override
                    public Addon initialize(ServiceConfig.Runtime config) {
                        startOrder.add(1);
                        return this;
                    }
                })
                .addon(new Addon() {
                    @Override
                    public Addon initialize(ServiceConfig.Runtime config) {
                        startOrder.add(2);
                        return this;
                    }
                });

        //When
        config.applyProperties(propertyMap);

        assertThat(startOrder).isEqualTo(Lists.newArrayList(1, 2));
    }

    @Test
    public void addons_may_specify_startup_order() {
        //Given
        final List<Integer> startOrder = Lists.newArrayList();

        class Dependee implements Addon {
            @Override
            public Addon initialize(ServiceConfig.Runtime config) {
                startOrder.add(1);
                return this;
            }
        }
        class Dependent implements Addon {
            @Override
            public Addon initialize(ServiceConfig.Runtime config) {
                startOrder.add(2);
                return this;
            }

            @Override
            public Set<Class<?>> initializeAfter() {
                return ImmutableSet.of(Dependee.class);
            }
        }

        ServiceConfig config = serviceConfig(serviceDefinition)
                .addon(new Dependent())
                .addon(new Dependee());

        //When
        config.applyProperties(propertyMap);

        assertThat(startOrder).isEqualTo(Lists.newArrayList(1, 2));
    }


    @Test
    public void if_dependencies_have_several_layers_use_multiple_dependencies() {
        //Given
        final List<Integer> startOrder = Lists.newArrayList();

        class Dependee implements Addon {
            @Override
            public Addon initialize(ServiceConfig.Runtime config) {
                startOrder.add(1);
                return this;
            }
        }
        class Immediate implements Addon {
            @Override
            public Addon initialize(ServiceConfig.Runtime config) {
                startOrder.add(2);
                return this;
            }

            @Override
            public Set<Class<?>> initializeAfter() {
                return ImmutableSet.of(Dependee.class);
            }
        }

        class Dependent implements Addon {
            @Override
            public Addon initialize(ServiceConfig.Runtime config) {
                startOrder.add(3);
                return this;
            }

            @Override
            public Set<Class<?>> initializeAfter() {
                return ImmutableSet.of(Immediate.class, Dependee.class);
            }
        }

        ServiceConfig config = serviceConfig(serviceDefinition)
                .addon(new Dependent())
                .addon(new Dependee())
                .addon(new Immediate());

        //When
        config.applyProperties(propertyMap);

        assertThat(startOrder).isEqualTo(Lists.newArrayList(1, 2, 3));
    }
}

