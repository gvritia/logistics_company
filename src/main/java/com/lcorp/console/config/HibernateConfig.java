package com.lcorp.console.config;

import com.lcorp.console.model.Client;
import com.lcorp.console.model.Driver;
import com.lcorp.console.model.Operator;
import com.lcorp.console.model.TransportationRequest;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.Objects;

public final class HibernateConfig {

    private HibernateConfig() {
    }

    public static SessionFactory createSessionFactory(DatabaseConfig config) {
        Objects.requireNonNull(config);

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting("jakarta.persistence.jdbc.driver", "org.postgresql.Driver")
            .applySetting("jakarta.persistence.jdbc.url", config.getJdbcUrl())
            .applySetting("jakarta.persistence.jdbc.user", config.getUsername())
            .applySetting("jakarta.persistence.jdbc.password", config.getPassword())
            .applySetting("hibernate.hbm2ddl.auto", "validate")
            .applySetting("hibernate.show_sql", "false")
            .applySetting("hibernate.connection.pool_size", "2")
            .build();

        try {
            return new MetadataSources(registry)
                .addAnnotatedClass(Client.class)
                .addAnnotatedClass(Driver.class)
                .addAnnotatedClass(Operator.class)
                .addAnnotatedClass(TransportationRequest.class)
                .buildMetadata()
                .buildSessionFactory();
        } catch (RuntimeException exception) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw exception;
        }
    }
}
