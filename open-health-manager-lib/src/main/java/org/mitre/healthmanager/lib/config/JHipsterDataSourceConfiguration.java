package org.mitre.healthmanager.lib.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "jhipsterEntityManagerFactory", transactionManagerRef = "jhipsterTransactionManager", basePackages = { "org.mitre.healthmanager" })
public class JHipsterDataSourceConfiguration {
    @Bean(name = "jhipsterDataSourceProperties")
    @ConfigurationProperties("application.jhipster.datasource") 
    public DataSourceProperties jhipsterDataSourceProperties() {
        return new DataSourceProperties();
    }
    
    @Bean(name = "jhipsterDataSource")
    @ConfigurationProperties(prefix = "application.jhipster.datasource.hikari")
    public DataSource jhipsterDataSource() {
    	return jhipsterDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = "jhipsterEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean jhipsterEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("jhipsterDataSource") DataSource jhipsterDataSource) {
        return builder.dataSource(jhipsterDataSource).packages("org.mitre.healthmanager").persistenceUnit("jhipster_PU").build();
    }

    @Bean(name = "jhipsterTransactionManager")
    public PlatformTransactionManager jhipsterTransactionManager(@Qualifier("jhipsterEntityManagerFactory") LocalContainerEntityManagerFactoryBean jhipsterEntityManagerFactory) {
        return new JpaTransactionManager(jhipsterEntityManagerFactory.getObject());
    }
}
