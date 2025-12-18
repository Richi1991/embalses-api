package com.app.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final HikariDataSource dataSource;

    static {
        try {
            Class.forName("org.postgresql.Driver");
            HikariConfig config = new HikariConfig();

            // Configuración de Neon (Usa variables de entorno en Render)
            config.setJdbcUrl(System.getenv("SPRING_DATASOURCE_URL"));
            config.setUsername(System.getenv("SPRING_DATASOURCE_USERNAME"));
            config.setPassword(System.getenv("SPRING_DATASOURCE_PASSWORD"));

            config.setMaximumPoolSize(5); // Neon Free tiene límites de conexiones, 5 es seguro
            config.setMinimumIdle(1);
            config.setIdleTimeout(300000); // 5 minutos
            config.setConnectionTimeout(20000); // 20 seg (margen para el cold start de Neon)
            config.setDriverClassName("org.postgresql.Driver");

            dataSource = new HikariDataSource(config);
        }catch (ClassNotFoundException e) {
            throw new RuntimeException("Error: No se encontró el driver de PostgreSQL en el classpath", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
