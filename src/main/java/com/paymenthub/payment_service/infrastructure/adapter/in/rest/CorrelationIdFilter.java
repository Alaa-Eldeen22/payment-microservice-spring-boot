package com.paymenthub.payment_service.infrastructure.adapter.in.rest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.paymenthub.payment_service.infrastructure.messaging.MessageContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Takes X-Correlation-Id (or makes one) so events from REST calls carry it. */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String correlationId = incoming != null && !incoming.isBlank() && incoming.length() <= 100
                ? incoming
                : UUID.randomUUID().toString();
        response.setHeader(HEADER, correlationId);

        try {
            MessageContext.run(new MessageContext.Data(correlationId, null), () -> {
                try {
                    chain.doFilter(request, response);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (ServletException e) {
                    throw new WrappedServletException(e);
                }
                return null;
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (WrappedServletException e) {
            throw (ServletException) e.getCause();
        }
    }

    private static class WrappedServletException extends RuntimeException {
        WrappedServletException(ServletException cause) {
            super(cause);
        }
    }
}
