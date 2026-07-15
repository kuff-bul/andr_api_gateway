package ru.adnr.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import reactor.core.publisher.Mono;

class UserLoginHeaderFilterTest {

    private final UserLoginHeaderFilter filter = new UserLoginHeaderFilter();

    @Test
    void addsUserLoginHeaderFromJwtPreferredUsername() {
        ServerWebExchange exchange = exchangeWithJwtLogin("user1", null);
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, captureExchange(downstreamExchange)).block();

        assertThat(downstreamExchange.get().getRequest().getHeaders())
                .containsEntry(UserLoginHeaderFilter.USER_LOGIN_HEADER, java.util.List.of("user1"));
    }

    @Test
    void replacesClientProvidedUserLoginHeader() {
        ServerWebExchange exchange = exchangeWithJwtLogin("user1", "fake");
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, captureExchange(downstreamExchange)).block();

        assertThat(downstreamExchange.get().getRequest().getHeaders()
                .getFirst(UserLoginHeaderFilter.USER_LOGIN_HEADER))
                .isEqualTo("user1");
    }

    @Test
    void removesTrustInClientProvidedHeaderWhenJwtHasNoLogin() {
        ServerWebExchange exchange = exchangeWithJwtClaims(Map.of("sub", "keycloak-user-id"), "fake");
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        filter.filter(exchange, captureExchange(downstreamExchange)).block();

        assertThat(downstreamExchange.get().getRequest().getHeaders()
                .getFirst(UserLoginHeaderFilter.USER_LOGIN_HEADER))
                .isNull();
    }

    private GatewayFilterChain captureExchange(AtomicReference<ServerWebExchange> downstreamExchange) {
        return exchange -> {
            downstreamExchange.set(exchange);
            return Mono.empty();
        };
    }

    private ServerWebExchange exchangeWithJwtLogin(String login, String inboundLoginHeader) {
        return exchangeWithJwtClaims(Map.of("preferred_username", login), inboundLoginHeader);
    }

    private ServerWebExchange exchangeWithJwtClaims(Map<String, Object> claims, String inboundLoginHeader) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest
                .get("/api/v1/files");
        if (inboundLoginHeader != null) {
            requestBuilder.header(UserLoginHeaderFilter.USER_LOGIN_HEADER, inboundLoginHeader);
        }

        MockServerWebExchange exchange = MockServerWebExchange.from(requestBuilder);
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                claims
        );
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        return new ServerWebExchangeDecorator(exchange) {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends Principal> Mono<T> getPrincipal() {
                return Mono.just((T) authentication);
            }
        };
    }
}
