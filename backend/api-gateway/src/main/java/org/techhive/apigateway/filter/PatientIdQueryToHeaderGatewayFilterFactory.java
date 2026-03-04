package org.techhive.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Copies the {@code patientId} query parameter to the {@code X-Patient-Id} request header
 * so downstream services receive it even when the request body is not forwarded.
 */
@Component
public class PatientIdQueryToHeaderGatewayFilterFactory
    extends AbstractGatewayFilterFactory<Object> {

  public static final String QUERY_PARAM = "patientId";
  public static final String HEADER_NAME = "X-Patient-Id";

  public PatientIdQueryToHeaderGatewayFilterFactory() {
    super(Object.class);
  }

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      String patientId = exchange.getRequest().getQueryParams().getFirst(QUERY_PARAM);
      if (patientId == null || patientId.isBlank()) {
        return chain.filter(exchange);
      }
      ServerHttpRequest mutated = exchange.getRequest().mutate()
          .header(HEADER_NAME, patientId.trim())
          .build();
      return chain.filter(exchange.mutate().request(mutated).build());
    };
  }
}
