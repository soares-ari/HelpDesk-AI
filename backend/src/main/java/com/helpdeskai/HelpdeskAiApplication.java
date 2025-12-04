package com.helpdeskai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Helpdesk AI - Sistema RAG Enterprise
 *
 * Aplicação Spring Boot para Q&A sobre documentação técnica usando RAG
 * (Retrieval-Augmented Generation) com pgvector e OpenAI.
 *
 * @author Seu Nome
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaRepositories
public class HelpdeskAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelpdeskAiApplication.class, args);

        System.out.println("""

                ╔═══════════════════════════════════════════════════════════════╗
                ║                                                               ║
                ║           🤖 Helpdesk AI - Backend Started 🚀                ║
                ║                                                               ║
                ║   RAG System for Technical Documentation Q&A                 ║
                ║   Stack: Spring Boot + Spring AI + pgvector + OpenAI         ║
                ║                                                               ║
                ║   📚 Swagger UI: http://localhost:8080/api/swagger-ui.html   ║
                ║   ❤️  Health Check: http://localhost:8080/actuator/health    ║
                ║                                                               ║
                ╚═══════════════════════════════════════════════════════════════╝

                """);
    }
}
