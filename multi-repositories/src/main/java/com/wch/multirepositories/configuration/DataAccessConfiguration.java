package com.wch.multirepositories.configuration;

import com.wch.multirepositories.entity.User;
import com.wch.multirepositories.readrepository.UserReadRepository;
import com.wch.multirepositories.repository.UserRepository;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.RepositoryBeanNamePrefix;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 数据库相关操作配置
 */
@Configuration
public class DataAccessConfiguration {

    @Autowired
    private JpaProperties properties;

    @Autowired
    private ObjectProvider<List<SchemaManagementProvider>> providers;
    @Autowired
    private ObjectProvider<PhysicalNamingStrategy> physicalNamingStrategy;
    @Autowired
    private ObjectProvider<ImplicitNamingStrategy> implicitNamingStrategy;

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.write")
    public DataSourceProperties writeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.write")
    public DataSource writeDataSource() {
        return writeDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.read")
    public DataSourceProperties readDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.read")
    public DataSource readDataSource() {
        return readDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean writeEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("writeDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages(User.class)
                .properties(getVendorProperties(dataSource))
                .persistenceUnit("write")
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean readEntityManagerFactory(
            EntityManagerFactoryBuilder builder, @Qualifier("readDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages(User.class)
                .properties(getVendorProperties(dataSource))
                .persistenceUnit("read")
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager writeTransactionManager(@Qualifier("writeEntityManagerFactory") LocalContainerEntityManagerFactoryBean writeEntityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager(writeEntityManagerFactory.getObject());
        return transactionManager;
    }

    @Bean
    public PlatformTransactionManager readTransactionManager(@Qualifier("readEntityManagerFactory") LocalContainerEntityManagerFactoryBean readEntityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager(readEntityManagerFactory.getObject());
        return transactionManager;
    }

    @EnableJpaRepositories(basePackageClasses = UserRepository.class,
            entityManagerFactoryRef = "writeEntityManagerFactory", transactionManagerRef = "writeTransactionManager")
    @Primary
    public class WriteConfiguration {
    }

    @EnableJpaRepositories(basePackageClasses = UserRepository.class,
            entityManagerFactoryRef = "readEntityManagerFactory", transactionManagerRef = "readTransactionManager")
    @RepositoryBeanNamePrefix("second")
    public class SameRepositoryWriteConfiguration {
    }

    @EnableJpaRepositories(basePackageClasses = UserReadRepository.class,
            entityManagerFactoryRef = "readEntityManagerFactory", transactionManagerRef = "readTransactionManager")
    public class ReadConfiguration {
    }

    protected Map<String, Object> getVendorProperties(DataSource dataSource) {
        Map<String, Object> vendorProperties = new LinkedHashMap<String, Object>();
        String defaultDdlMode = new HibernateDefaultDdlAutoProvider(
                providers.getIfAvailable(Collections::emptyList))
                .getDefaultDdlAuto(dataSource);
        vendorProperties.putAll(properties.getHibernateProperties(
                new HibernateSettings().ddlAuto(defaultDdlMode).physicalNamingStrategy(physicalNamingStrategy.getIfAvailable())
                        .implicitNamingStrategy(implicitNamingStrategy.getIfAvailable())
        ));
        return vendorProperties;
    }

    class HibernateDefaultDdlAutoProvider implements SchemaManagementProvider {

        private final List<SchemaManagementProvider> providers;

        HibernateDefaultDdlAutoProvider(List<SchemaManagementProvider> providers) {
            this.providers = providers;
        }

        public String getDefaultDdlAuto(DataSource dataSource) {
            if (!EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
                return "none";
            }
            SchemaManagement schemaManagement = getSchemaManagement(dataSource);
            if (SchemaManagement.MANAGED.equals(schemaManagement)) {
                return "none";
            }
            return "create-drop";

        }

        @Override
        public SchemaManagement getSchemaManagement(DataSource dataSource) {
            for (SchemaManagementProvider provider : this.providers) {
                SchemaManagement schemaManagement = provider.getSchemaManagement(dataSource);
                if (SchemaManagement.MANAGED.equals(schemaManagement)) {
                    return schemaManagement;
                }
            }
            return SchemaManagement.UNMANAGED;
        }

    }

}
