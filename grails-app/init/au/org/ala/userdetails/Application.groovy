package au.org.ala.userdetails

import au.org.ala.recaptcha.RecaptchaClient
import com.mongodb.MongoClientSettings
//import com.mongodb.client.MongoClient // Switch out on upgrade
import com.mongodb.MongoClient
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.util.logging.Slf4j
import okhttp3.OkHttpClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoClientFactory
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.session.data.mongo.JdkMongoSessionConverter
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

import javax.sql.DataSource
import java.time.Duration
import java.util.stream.Collectors

@Slf4j
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    DataSourceHealthIndicator dataSourceHealthIndicator(DataSource dataSource) {
        new DataSourceHealthIndicator(dataSource)
    }

    @Bean
    RecaptchaClient recaptchaClient() {
        def baseUrl = grailsApplication.config.getProperty('recaptcha.baseUrl', 'https://www.google.com/recaptcha/api/')
        return new Retrofit.Builder().baseUrl(baseUrl).client(new OkHttpClient()).addConverterFactory(MoshiConverterFactory.create()).build().create(RecaptchaClient)
    }

    @Configuration
    @ConditionalOnProperty(value = "spring.session.enabled", havingValue = "true", matchIfMissing = false)
    @Import(MongoAutoConfiguration) // unsure if this is disabled by grails?
    @EnableMongoHttpSession
    @EnableConfigurationProperties(MongoProperties)
    static class MongoSessionConfig {

//        @Bean
//        @Qualifier("mongoOperations")
//        @Primary
//        MongoOperations mongoOperations(MongoDbFactory mongoDatabaseFactory) {
//            return new MongoTemplate(mongoDatabaseFactory);
//        }
//
//        @Bean
//        MongoDbFactory mongoDatabaseFactory(MongoClient mongoClient, MongoProperties properties) {
//            return new SimpleMongoClientDbFactory(mongoClient, properties.mongoClientDatabase)
//        }

        @Bean
        JdkMongoSessionConverter jdkMongoSessionConverter() {
            return new JdkMongoSessionConverter(Duration.ofMinutes(15L));
        }

//        @Bean
//        @ConditionalOnMissingBean(MongoClient.class)
//        MongoClient mongoClient(MongoProperties properties, Environment environment,
//                                 ObjectProvider<MongoClientSettings> settings) {
//            return new MongoClientFactory(properties, environment).createMongoClient(settings.getIfAvailable());
//        }
//
//        @Bean
//        @ConditionalOnMissingBean(MongoClientSettings.class)
//        MongoClientSettings mongoClientSettings(ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers)  {
//            def builder = MongoClientSettings.builder()
//            for (def customizer : builderCustomizers) {
//                customizer.customize(builder)
//            }
//            return builder.build();
//        }

        // Spring Boot 2.5
//    @Bean
//    @ConditionalOnMissingBean(MongoClient.class)
//    MongoClient mongo(
//            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
//            MongoClientSettings settings
//    ) {
//        return new MongoClientFactory(builderCustomizers.orderedStream().collect(Collectors.toList()))
//                .createMongoClient(settings);
//    }

        // Spring Boot 2.5
//    @Bean
//    MongoPropertiesClientSettingsBuilderCustomizer mongoPropertiesCustomizer(
//            MongoProperties properties,
//            Environment environment
//    ) {
//        return MongoPropertiesClientSettingsBuilderCustomizer(properties, environment)
//    }
    }

}