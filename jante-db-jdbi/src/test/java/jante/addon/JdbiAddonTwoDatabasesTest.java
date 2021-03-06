package jante.addon;

import com.google.common.collect.Lists;
import jante.ServiceDefinitionUtil;
import jante.TestServiceRunner;
import jante.ServiceConfig;
import org.junit.Test;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static jante.Injections.injections;
import static jante.ServiceConfig.serviceConfig;
import static jante.ServiceDefinitionUtil.stubServiceDefinition;
import static jante.addon.H2InMemoryDatasourceAddon.h2InMemoryDatasourceAddon;
import static jante.addon.JdbiAddon.jdbiAddon;
import static org.assertj.core.api.Assertions.assertThat;

public class JdbiAddonTwoDatabasesTest {


    ServiceConfig serviceConfig = serviceConfig(ServiceDefinitionUtil.stubServiceDefinition(Api.class))
            .addon(ExceptionMapperAddon.exceptionMapperAddon)
            .addon(h2InMemoryDatasourceAddon.name(addon_name)
                    .script("CREATE TABLE testable (id INTEGER, name VARCHAR);")
                    .insert("testable", 101, "'Per'")
                    .insert("testable", 303, "'Espen'")
                    .script("INSERT INTO testable VALUES (202, 'Per');")
            )
            .addon(jdbiAddon.dao(JdbiDto.class).name(addon_name))
            .addon(h2InMemoryDatasourceAddon.name(addon_name2)
                    .script("CREATE TABLE mongoable (id VARCHAR, name VARCHAR);")
                    .insert("mongoable", "'eple'", "'Per'")
                    .insert("mongoable", "'kake'", "'Espen'")
                    .insert("mongoable", "'bil'", "'Per'")
            )
            .addon(jdbiAddon.dao(JdbiDto2.class).name(addon_name2))
            .inject(props -> injections
                    .bind(ApiImpl.class, Api.class)
                    .bind(ApiImpl2.class, Api2.class)
            );


    @Test
    public void runsWithJdbi() {
        List<Integer> expected = Lists.newArrayList(101, 202);
        List<Integer> actual = TestServiceRunner.testServiceRunner(serviceConfig)
                .oneShot(Api.class, Api::get);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testChainExample() {
        List<Integer> expected = Lists.newArrayList(101, 202);
        List<String> expected2 = Lists.newArrayList("eple", "bil");
        TestServiceRunner.testServiceRunner(serviceConfig)
                .chain()
                .call(Api.class, Api::get)
                .injectee(JdbiDto.class, it -> assertThat(it.doGet("Per")).isEqualTo(expected))
                .injectee(JdbiDto2.class, it -> assertThat(it.doGet2("Per")).isEqualTo(expected2))
                .run();
    }

    public interface JdbiDto {

        @SqlQuery("SELECT\n"
                + "  id\n"
                + "FROM testable \n"
                + "WHERE name = :param\n")
        List<Integer> doGet(@Bind("param") String param);
    }


    public @Path("")
    interface Api {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        List<Integer> get();
    }


    public static class ApiImpl implements Api {
        @Inject
        JdbiDto jdbiDto;

        public List<Integer> get() {
            return jdbiDto.doGet("Per");
        }
    }


    static final String addon_name2 = "Eple";


    public interface JdbiDto2 {

        @SqlQuery("SELECT\n"
                + "  id\n"
                + "FROM mongoable \n"
                + "WHERE name = :param\n")
        List<String> doGet2(@Bind("param") String param);
    }


    public @Path("")
    interface Api2 {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        List<String> get2();
    }


    public static class ApiImpl2 implements Api2 {
        @Inject
        JdbiDto2 jdbiDto;

        public List<String> get2() {
            return jdbiDto.doGet2("Per");
        }
    }


    static final String addon_name = "Banan";
}
