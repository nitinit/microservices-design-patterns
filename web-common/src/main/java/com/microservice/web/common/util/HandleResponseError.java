package com.microservice.web.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Handle http status according to type of Exception.
 */
@Slf4j
@AllArgsConstructor
public class HandleResponseError {
    private final CustomDefaultErrorAttributes customDefaultErrorAttributes;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Set http status according to Exception and print exception message when writeToResponse is true.
     * @param exchange the current request
     * @param ex the current exception
     * @param writeToResponse print exception message to response when is true
     * @return Mono<Void>
     */
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex, boolean writeToResponse) {
        log.error("Error on calling api", ex);
        log.error("Error on calling api:request: {}", exchange.getRequest().getPath().value());
        ServerHttpResponse response = exchange.getResponse();
        HttpStatus httpStatus = customDefaultErrorAttributes.getHttpStatusError(ex);
        response.setStatusCode(httpStatus);
        if (writeToResponse) {
            byte[] bytes = getBytes(ex);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            DataBufferUtils.release(buffer);
            return response.writeWith(Flux.just(buffer));
        } else {
            return Mono.empty();
        }
    }

    private byte[] getBytes(Throwable ex) {
        try {
            return objectMapper.writeValueAsString(Collections.singletonMap("message", ExceptionUtils.getMessage(ex))).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            log.error("Error converting to bytes", e);
            throw new RuntimeException(e);
        }
    }

    public byte[] convertToByteArray(Object object) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}