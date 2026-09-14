package com.queue.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket je stalno otvorena veza izmedju browsera i servera.
 * Za razliku od HTTP-a (pitaj -> dobij odgovor -> veza se zatvori),
 * ovdje veza ostaje, pa SERVER moze prvi progovoriti.
 *
 * STOMP je dogovor o tome kako poruke izgledaju - bez njega bi kroz
 * WebSocket isli goli bajtovi bez ikakve strukture.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Adresa na koju se browser kaci. Jedna veza po klijentu,
        // preko nje idu sve pretplate.
        registry.addEndpoint("/ws")
                // U razvoju frontend je na 4200, backend na 8080 - za
                // WebSocket proxy dev servera to je i dalje drugi origin.
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker u memoriji. Sve sto krene sa /topic on razasalje svima
        // koji su na to pretplaceni. Dovoljno dok je jedna instanca servera;
        // za vise instanci bi trebao pravi broker (RabbitMQ - V5).
        registry.enableSimpleBroker("/topic");

        // Prefiks za poruke koje klijent SALJE serveru (@MessageMapping).
        // Nama ne treba - komunikacija ide samo server -> klijent,
        // klijent i dalje mijenja stanje preko obicnog REST-a.
        registry.setApplicationDestinationPrefixes("/app");
    }
}
