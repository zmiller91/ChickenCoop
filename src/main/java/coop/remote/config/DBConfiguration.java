package coop.remote.config;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.google.common.base.Strings;
import com.google.gson.Gson;
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
import java.net.URLEncoder;

@Log4j2
@Configuration
public class DBConfiguration {

    @Bean
    DataSource dataSource(@Value("${db.creds}") String creds, @Value("${db.user}") String username, @Value("${db.password}") String password) {

        log.error("ZZZ This should be present in the logs.");

        DataSourceBuilder builder = DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver");
        log.info("ZZZ Are creds present: " + !Strings.isNullOrEmpty(creds));
        if(!Strings.isNullOrEmpty(creds)) {

            log.info("ZZZ Connecting to DB via secrets manager");

            AWSSecretsManager secrets = AWSSecretsManagerClientBuilder.defaultClient();

            GetSecretValueRequest request = new GetSecretValueRequest();
            request.setSecretId(creds);
            GetSecretValueResult response = secrets.getSecretValue(request);

            JsonObject info = JsonParser.parseString(response.getSecretString()).getAsJsonObject();

            String u = info.get("username").getAsString();
            String p = info.get("password").getAsString();
            String h = info.get("host").getAsString();

            log.info("ZZZ Username: " + u);
            log.info("ZZZ Password: " + p);
            log.info("ZZZ URL: " + "jdbc:mysql://" + h + "/local_pi");

            builder.username(u)
                    .password(p)
                    .url("jdbc:mysql://" + h + ":3306/local_pi");

        } else {
            builder.username(username)
                    .password(password)
                    .url("jdbc:mysql://localhost:3306/local_pi");
        }



        return builder.build();
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
