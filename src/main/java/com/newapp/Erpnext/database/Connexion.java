package com.newapp.Erpnext.database;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class Connexion {
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://172.29.141.30:3306/_ce60f8318a59a628");
        dataSource.setUsername("springuser");
        dataSource.setPassword("1234");
        return dataSource;
    }
}
