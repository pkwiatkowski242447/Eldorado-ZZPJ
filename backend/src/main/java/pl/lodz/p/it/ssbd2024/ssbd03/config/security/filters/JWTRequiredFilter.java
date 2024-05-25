package pl.lodz.p.it.ssbd2024.ssbd03.config.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.lodz.p.it.ssbd2024.ssbd03.commons.dto.exception.ExceptionDTO;
import pl.lodz.p.it.ssbd2024.ssbd03.config.security.consts.SecurityConstants;
import pl.lodz.p.it.ssbd2024.ssbd03.utils.I18n;

import java.io.IOException;
import java.io.OutputStream;

@Slf4j
@Component
public class JWTRequiredFilter extends OncePerRequestFilter {

    private static final String[] WHITELIST = {
        "/api/v1/auth/login-credentials",
        "/api/v1/auth/login-auth-code",
        "/api/v1/register/client",
        "/api/v1/accounts/forgot-password",
        "/api/v1/accounts/change-password",
        "/api/v1/accounts/activate-account",
        "/api/v1/accounts/confirm-email",
        "/v3/api-docs",
        "/swagger-ui",
        "/swagger-resources",
        "/configuration/ui",
        "/configuration/security",
        "/swagger-ui.html",
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            ObjectMapper objectMapper = new ObjectMapper();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            try (OutputStream outputStream = response.getOutputStream()) {
                outputStream.write(objectMapper.writeValueAsBytes(new ExceptionDTO(I18n.UNAUTHORIZED_EXCEPTION)));
            }
            response.getWriter().flush();
            SecurityContextHolder.clearContext();
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        for (String key : WHITELIST) {
            if (request.getRequestURI().startsWith(key)) return true;
        }
        return false;
    }
}