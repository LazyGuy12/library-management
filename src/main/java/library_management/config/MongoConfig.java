package library_management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

/**
 * Configuration cho MongoDB - Đăng ký custom converters
 * Sử dụng để map old string status sang new LoanStatus enum
 */
@Configuration
public class MongoConfig {

    /**
     * Đăng ký custom converters
     */
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
            new LoanStatusConverter()  // Custom converter để map String → LoanStatus enum
        ));
    }
}
