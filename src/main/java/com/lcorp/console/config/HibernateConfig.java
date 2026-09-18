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
    // запрещаем создать объект
    private HibernateConfig() {
    }

    // SessionFactory создаётся один раз на всё приложение и передаётся репозиториям
    // Владелец фабрики (Main) закрывает её при завершении приложения
    public static SessionFactory createSessionFactory(DatabaseConfig config) {
        Objects.requireNonNull(config);

        // Параметры берём из того же источника, что и для ручного JDBC
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting("jakarta.persistence.jdbc.driver", "org.postgresql.Driver")
            .applySetting("jakarta.persistence.jdbc.url", config.getJdbcUrl())
            .applySetting("jakarta.persistence.jdbc.user", config.getUsername())
            .applySetting("jakarta.persistence.jdbc.password", config.getPassword())
            // Проверяем таблицы при запуске, а создание и изменение остаётся в SQL
            .applySetting("hibernate.hbm2ddl.auto", "validate")
            .applySetting("hibernate.show_sql", "false")
            // Встроенный небольшой пул
            // В backend управление пулом подключений возьмёт на себя Spring Boot
            .applySetting("hibernate.connection.pool_size", "2")
            .build();

        try {
            // говорим какие классы являются таблицами
            return new MetadataSources(registry)
                .addAnnotatedClass(Client.class)
                .addAnnotatedClass(Driver.class)
                .addAnnotatedClass(Operator.class)
                .addAnnotatedClass(TransportationRequest.class)
                .buildMetadata()
                .buildSessionFactory(); // создание сессии для работы с бд
        } catch (RuntimeException exception) {
            // Если запуск не удался, фабрика ещё не существует: освобождаем registry сами
            StandardServiceRegistryBuilder.destroy(registry);
            throw exception;
        }
    }
}
