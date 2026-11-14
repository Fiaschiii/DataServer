package com.dataserver.dataserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@SpringBootApplication
public class DataserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataserverApplication.class, args);
	}

    // Sites que podem chamar a API pelo navegador (CORS_ORIGENS no .env / no Render,
    // separados por vírgula). Padrão: o Vite local.
    @Bean
	public WebMvcConfigurer corsConfigurer(@Value("${cors.origens}") String[] origens) {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
				        .allowedOrigins(origens)
				        .allowedMethods("GET", "POST", "PUT", "DELETE");
			}
		};
	}
}




