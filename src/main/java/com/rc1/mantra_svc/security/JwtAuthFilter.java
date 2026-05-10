package com.rc1.mantra_svc.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive JWT authentication filter.
 * Extracts the Bearer token from the Authorization header,
 * validates it, and sets the authenticated principal in the
 * reactive security context.
 *
 * <p>Uses thenReturn/defaultIfEmpty instead of switchIfEmpty on Mono&lt;Void&gt;.
 * A Mono&lt;Void&gt; NEVER emits items (Void has no instances), so
 * {@code .switchIfEmpty(chain.filter(exchange))} would ALWAYS fire — invoking
 * the filter chain a second time without authentication.  For endpoints that
 * return a response body the race is lost silently (response already committed),
 * but for PUT/DELETE with NO_CONTENT the response is not yet committed when the
 * second chain call runs, so Spring Security wins with 401.
 * The fix: convert to Mono&lt;Boolean&gt; inside flatMap so the downstream can
 * distinguish "chain ran with auth" from "user not found / token invalid".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtUtil.extractEmail(token);
            return userDetailsService.findByUsername(email)
                    .filter(ud -> jwtUtil.isTokenValid(token, ud.getUsername()))
                    .flatMap(ud -> {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                        // thenReturn(TRUE) converts Mono<Void> → Mono<Boolean> so
                        // defaultIfEmpty(FALSE) can tell whether auth succeeded.
                        // Without this, switchIfEmpty on Mono<Void> would always fire.
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                                .thenReturn(Boolean.TRUE);
                    })
                    .defaultIfEmpty(Boolean.FALSE)
                    .flatMap(wasAuthenticated -> wasAuthenticated
                            ? Mono.<Void>empty()       // chain already ran with auth — nothing left to do
                            : chain.filter(exchange)); // token absent/invalid — proceed unauthenticated
        } catch (Exception e) {
            log.debug("JWT filter skipped: {}", e.getMessage());
            return chain.filter(exchange);
        }
    }
}
