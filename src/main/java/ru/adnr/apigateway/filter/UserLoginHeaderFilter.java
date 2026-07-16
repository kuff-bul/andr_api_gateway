package ru.adnr.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserLoginHeaderFilter implements GlobalFilter, Ordered {

    public static final String USER_LOGIN_HEADER = "X-User-Login";

    private static final String LOGIN_CLAIM = "preferred_username";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitizedExchange = withoutUserLoginHeader(exchange);
        return sanitizedExchange.getPrincipal()
                .ofType(Authentication.class)
                .flatMap(authentication -> Mono.justOrEmpty(extractLogin(authentication)))
                .filter(StringUtils::hasText)
                .map(login -> withUserLoginHeader(sanitizedExchange, login))
                .defaultIfEmpty(sanitizedExchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private ServerWebExchange withUserLoginHeader(ServerWebExchange exchange, String login) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(USER_LOGIN_HEADER, login)))
                .build();
    }

    private ServerWebExchange withoutUserLoginHeader(ServerWebExchange exchange) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> headers.remove(USER_LOGIN_HEADER)))
                .build();
    }

    private String extractLogin(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getClaimAsString(LOGIN_CLAIM);
        }
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getClaimAsString(LOGIN_CLAIM);
        }
        if (principal instanceof OAuth2User oauth2User) {
            Object login = oauth2User.getAttributes().get(LOGIN_CLAIM);
            return login instanceof String value ? value : null;
        }
        return null;
    }
}
