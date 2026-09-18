package com.lcorp.console;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.config.HibernateConfig;
import com.lcorp.console.repository.ClientRepository;
import com.lcorp.console.repository.hibernate.HibernateClientRepository;
import jakarta.persistence.PersistenceException;
import org.hibernate.SessionFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        try {
            DatabaseConfig config = DatabaseConfig.load();
            DatabaseConnectionFactory connectionFactory =
                new DatabaseConnectionFactory(config);

            try (Connection connection = connectionFactory.openConnection()) {
                if (!connection.isValid(5)) {
                    throw new SQLException("PostgreSQL не подтвердил соединение");
                }
                System.out.println("Подключение к PostgreSQL установлено");
            }

            // Фабрика общая для будущих ORM-репозиториев; здесь пока проверяем чтение
            try (SessionFactory sessionFactory = HibernateConfig.createSessionFactory(config)) {
                ClientRepository clients = new HibernateClientRepository(sessionFactory);
                System.out.println("Hibernate: соответствие моделей таблицам проверено");
                System.out.println("Клиентов в базе: " + clients.findAll().size());
            }
        } catch (IllegalStateException | SQLException | PersistenceException exception) {
            System.err.println(
                "Ошибка запуска JDBC/Hibernate: " + exception.getMessage()
            );
            System.exit(1);
        }
    }
}
