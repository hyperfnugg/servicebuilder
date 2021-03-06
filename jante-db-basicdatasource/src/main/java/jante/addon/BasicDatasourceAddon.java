package jante.addon;

import com.google.common.base.Strings;
import jante.Injections;
import jante.JettyServer;
import jante.ServiceConfig;
import jante.model.Addon;
import jante.model.PropertyProvider;
import jante.util.ObosHealthCheckRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Wither;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;

/**
 * Knytter opp en datakilde og binder BasicDatasource og QueryRunner til hk2.
 * Ved initialisering (defaults og config) kan det legges til et navn til datakilden
 * for å støtte flere datakilder. Parametre fre properties vil da leses fra
 * navnet (databasenavn).db.url osv.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BasicDatasourceAddon implements DataSourceAddon {

    public static final String CONFIG_KEY_DB_URL = "db.url";
    public static final String CONFIG_KEY_DB_DRIVER_CLASS_NAME = "db.driverClassName";
    public static final String CONFIG_KEY_DB_USERNAME = "db.username";
    public static final String CONFIG_KEY_DB_PASSWORD = "db.password";
    public static final String CONFIG_KEY_DB_VALIDATION_QUERY = "db.validationQuery";

    @Getter
    @Wither(AccessLevel.PRIVATE)
    public final String name;
    @Wither(AccessLevel.PRIVATE)
    public final String url;
    @Wither(AccessLevel.PRIVATE)
    public final String driverClassName;
    @Wither(AccessLevel.PRIVATE)
    public final String username;
    @Wither(AccessLevel.PRIVATE)
    public final String password;
    @Wither(AccessLevel.PRIVATE)
    public final String validationQuery;
    @Wither(AccessLevel.PRIVATE)
    public final boolean monitorIntegration;
    @Getter
    @Wither(AccessLevel.PRIVATE)
    public final DataSource dataSource;

    public static BasicDatasourceAddon basicDatasourceAddon = new BasicDatasourceAddon(null, null, null, null, null, "select 1", true, null);

    @Override
    public Addon initialize(ServiceConfig.Runtime config) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setValidationQuery(validationQuery);

        return this.withDataSource(dataSource);
    }

    @Override
    public Addon withProperties(PropertyProvider properties) {
        String prefix = Strings.isNullOrEmpty(name) ? "" : name + ".";
        return this
                .url(properties.requireWithFallback(prefix + CONFIG_KEY_DB_URL, url))
                .username(properties.requireWithFallback(prefix + CONFIG_KEY_DB_USERNAME, username))
                .password(properties.requireWithFallback(prefix + CONFIG_KEY_DB_PASSWORD, password))
                .driverClassName(properties.requireWithFallback(prefix + CONFIG_KEY_DB_DRIVER_CLASS_NAME, driverClassName))
                .validationQuery(properties.requireWithFallback(prefix + CONFIG_KEY_DB_VALIDATION_QUERY, validationQuery));
    }


    @Override
    public Injections getInjections() {
        if (!Strings.isNullOrEmpty(name)) {
            return Injections.injections
                    .bindNamed(dataSource, DataSource.class, name);
        } else {
            return Injections.injections
                    .bind(dataSource, DataSource.class);
        }
    }

    @Override
    public JettyServer addToJettyServer(JettyServer jettyServer) {
        if (monitorIntegration) {
            String dataSourceName = Strings.isNullOrEmpty(name)
                    ? " (" + name + ")"
                    : "";
            ObosHealthCheckRegistry.registerDataSourceCheck("Database" + dataSourceName + ": " + url, dataSource, validationQuery);
        }
        return jettyServer;
    }

    public BasicDatasourceAddon name(String name) {
        return withName(name);
    }

    public BasicDatasourceAddon url(String url) {
        return withUrl(url);
    }

    public BasicDatasourceAddon driverClassName(String driverClassName) {
        return withDriverClassName(driverClassName);
    }

    public BasicDatasourceAddon username(String username) {
        return withUsername(username);
    }

    public BasicDatasourceAddon password(String password) {
        return withPassword(password);
    }

    public BasicDatasourceAddon validationQuery(String validationQuery) {
        return withValidationQuery(validationQuery);
    }

    public BasicDatasourceAddon monitorIntegration(boolean monitorIntegration) {
        return withMonitorIntegration(monitorIntegration);
    }
}
