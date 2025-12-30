package coop.local.config;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class DBConfiguration {

    @Bean
    @Primary
    DataSource dataSource(@Value("${db.user}") String username, @Value("${db.password}") String password) {
        return DataSourceBuilder.create()
                .username(username)
                .password(password)
                .url("jdbc:mysql://localhost:3306/local_pi")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    @Bean
    @Primary
    public SessionFactory sessionFactory(DataSource dataSource) {
        LocalSessionFactoryBuilder builder = new LocalSessionFactoryBuilder(dataSource);
        builder.scanPackages("coop.shared.*", "coop.local.*");
        return builder.buildSessionFactory();
    }


    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }

    @Bean
    @Qualifier("piDataSource")
    DataSource piDataSource(@Value("${db.user}") String username, @Value("${db.password}") String password) {
        return DataSourceBuilder.create()
                .username(username)
                .password(password)
                .url("jdbc:mysql://localhost:3306/pi_state")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    @Bean
    @Qualifier("piDataSource")
    public SessionFactory piSessionFactory(@Qualifier("piDataSource") DataSource dataSource) {
        LocalSessionFactoryBuilder builder = new LocalSessionFactoryBuilder(dataSource);
        builder.scanPackages("coop.shared.*", "coop.local.*");
        return builder.buildSessionFactory();
    }

    @Bean
    @Qualifier("piTransactionManager")
    public PlatformTransactionManager piTransactionManager(@Qualifier("piDataSource") SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }
}
