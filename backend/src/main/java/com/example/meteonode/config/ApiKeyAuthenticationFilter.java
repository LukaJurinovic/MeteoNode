package com.example.meteonode.config;

import com.example.meteonode.model.entity.Gateway;
import com.example.meteonode.service.domain.GatewayService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String GATEWAY_URL_HEADER = "X-Gateway-Url";

    private final GatewayService gatewayService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/gateway/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        Optional<Gateway> gateway = apiKey != null
                ? gatewayService.findByApiKey(apiKey)
                : Optional.empty();

        if (gateway.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing API key");
            return;
        }

        updateGatewayUrlIfPresent(gateway.get(), request.getHeader(GATEWAY_URL_HEADER));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                apiKey, null, List.of(new SimpleGrantedAuthority("ROLE_GATEWAY"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }

    private void updateGatewayUrlIfPresent(Gateway gateway, String gatewayUrl) {
        if (gatewayUrl == null || gatewayUrl.equals(gateway.getGatewayUrl())) return;
        gatewayService.updateUrl(gateway.getId(), gatewayUrl);
    }
}
