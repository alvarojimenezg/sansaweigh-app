package cl.sansaweigh.sansaweighapp.configs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentación OpenAPI / Swagger UI.
 *
 * Swagger UI:  http://localhost:8080/swagger-ui.html
 * OpenAPI JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sansaWeighOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SansaWeigh API")
                        .description("Microservicio de estaciones de pesaje: clasifica paquetes por peso, "
                                + "aplica reglas de negocio, persiste el historial en MongoDB y cachea "
                                + "especificaciones de balanza en Redis con fallback a la API externa.")
                        .version("0.0.1-SNAPSHOT")
                        .contact(new Contact().name("Equipo SansaWeigh"))
                        .license(new License().name("TallerHDD")));
    }
}
