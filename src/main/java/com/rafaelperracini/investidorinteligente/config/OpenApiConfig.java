package com.rafaelperracini.investidorinteligente.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Investidor Inteligente API")
                        .version("1.0.0")
                        .description("API de analise fundamentalista de acoes brasileiras com IA. " +
                                "Busca dados em tempo real na brapi.dev, analisa fundamentos com " +
                                "Ollama/Llama 3.2 local e emite pareceres de investimento.")
                        .contact(new Contact()
                                .name("Rafael Perracini")
                                .url("https://github.com/perracini")));
    }
}
