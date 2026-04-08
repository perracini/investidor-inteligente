package com.rafaelperracini.investidorinteligente.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() throws Exception {
        return EmbeddedPostgres.start().getPostgresDatabase();
    }
}
