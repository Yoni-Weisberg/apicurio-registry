package io.apicurio.registry.storage.impl.sql;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration.TransactionRequirement;
import io.agroal.api.configuration.supplier.AgroalPropertiesReader;
import io.apicurio.common.apps.config.Info;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class RegistryDatasourceProducer {

    @Inject
    Logger log;

    @ConfigProperty(name = "apicurio.storage.sql.kind", defaultValue = "h2")
    @Info(category = "storage", description = "Application datasource database type", availableSince = "3.0.0")
    String databaseType;

    @ConfigProperty(name = "apicurio.datasource.url", defaultValue = "jdbc:h2:mem:registry_db")
    @Info(category = "storage", description = "Application datasource jdbc url", availableSince = "3.0.0")
    String jdbcUrl;

    @ConfigProperty(name = "apicurio.datasource.username", defaultValue = "sa")
    @Info(category = "storage", description = "Application datasource username", availableSince = "3.0.0")
    String username;

    @ConfigProperty(name = "apicurio.datasource.password", defaultValue = "sa")
    @Info(category = "storage", description = "Application datasource password", availableSince = "3.0.0")
    String password;

    @ConfigProperty(name = "apicurio.datasource.jdbc.initial-size", defaultValue = "20")
    @Info(category = "storage", description = "Application datasource pool initial size", availableSince = "3.0.0")
    String initialSize;

    @ConfigProperty(name = "apicurio.datasource.jdbc.min-size", defaultValue = "20")
    @Info(category = "storage", description = "Application datasource pool minimum size", availableSince = "3.0.0")
    String minSize;

    @ConfigProperty(name = "apicurio.datasource.jdbc.max-size", defaultValue = "100")
    @Info(category = "storage", description = "Application datasource pool maximum size", availableSince = "3.0.0")
    String maxSize;

    @Produces
    @ApplicationScoped
    @Named("application")
    public AgroalDataSource produceDatasource() throws SQLException {
        log.debug("Creating an instance of ISqlStatements for DB: " + databaseType);

        final RegistryDatabaseKind databaseKind = RegistryDatabaseKind.valueOf(databaseType);

        Map<String, String> props = new HashMap<>();

        props.put(AgroalPropertiesReader.MAX_SIZE, maxSize);
        props.put(AgroalPropertiesReader.MIN_SIZE, minSize);
        props.put(AgroalPropertiesReader.INITIAL_SIZE, initialSize);
        props.put(AgroalPropertiesReader.JDBC_URL, jdbcUrl);
        props.put(AgroalPropertiesReader.PRINCIPAL, username);
        props.put(AgroalPropertiesReader.CREDENTIAL, password);
        props.put(AgroalPropertiesReader.PROVIDER_CLASS_NAME, databaseKind.getDriverClassName());

        /*
         * We need to disable auto-commit to have proper transaction rollback, otherwise the data may be in an
         * inconsistent state (e.g. partial deletes), or operations would not be rolled back on exception.
         */
        props.put(AgroalPropertiesReader.AUTO_COMMIT, "false");
        props.put(AgroalPropertiesReader.TRANSACTION_ISOLATION, TransactionIsolation.READ_COMMITTED.name());
        props.put(AgroalPropertiesReader.TRANSACTION_REQUIREMENT, TransactionRequirement.WARN.name());
        props.put(AgroalPropertiesReader.FLUSH_ON_CLOSE, "true");

        AgroalDataSource datasource = AgroalDataSource
                .from(new AgroalPropertiesReader().readProperties(props).get());

        log.info("Using {} SQL storage.", databaseType);

        return datasource;
    }
}
