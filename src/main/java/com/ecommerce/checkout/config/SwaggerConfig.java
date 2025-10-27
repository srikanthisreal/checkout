package com.ecommerce.checkout.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI checkoutOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce Checkout API")
                        .description("API for handling shopping cart and checkout operations")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@ecommerce.com")));
    }
}
