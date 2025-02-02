package coop.remote.config;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.log4j.Log4j2;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Log4j2
@Configuration
public class DBConfiguration {

    @Bean
    DataSource dataSource(
            @Value("${db.host}") String host,
            @Value("${db.port}") String port,
            @Value("${db.user}") String username,
            @Value("${db.password}") String password,
            @Value("${db.name}") String db,
            @Value("${db.secret_store}") String secretStore) {

        if(!Strings.isNullOrEmpty(secretStore)) {
            AWSSecretsManager secrets = AWSSecretsManagerClientBuilder.defaultClient();
            GetSecretValueRequest request = new GetSecretValueRequest();
            request.setSecretId(secretStore);
            GetSecretValueResult response = secrets.getSecretValue(request);

            JsonObject info = JsonParser.parseString(response.getSecretString()).getAsJsonObject();
            username = info.get("username").getAsString();
            password = info.get("password").getAsString();
            host = info.get("host").getAsString();
        }

        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .username(username)
                .password(password)
                .url("jdbc:mysql://" + host + ":" + port + "/" + db)
                .build();
    }

    @Bean
    public SessionFactory sessionFactory(DataSource dataSource) {
        LocalSessionFactoryBuilder builder = new LocalSessionFactoryBuilder(dataSource);
        builder.scanPackages("coop.shared.*", "coop.remote.*");
        return builder.buildSessionFactory();
    }

    @Bean
    public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }
}
