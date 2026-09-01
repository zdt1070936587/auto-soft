package com.autosoft.framework.security;

import com.autosoft.common.core.ResultCode;
import com.autosoft.common.exception.BizException;
import com.autosoft.framework.security.jwt.JwtManager;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Collections;

/**
 * 解析 Bearer JWT 并写入 SecurityContext。权限每次查库。
 *
 * @author zhaodt
 * @since 2026-08-31
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtManager jwtManager;
    private final LoginUserLoader loginUserLoader;
    private final JsonMapper jsonMapper;

    public JwtAuthenticationFilter(JwtManager jwtManager, LoginUserLoader loginUserLoader, JsonMapper jsonMapper) {
        this.jwtManager = jwtManager;
        this.loginUserLoader = loginUserLoader;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path)
                || "/api/health".equals(path)
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(PREFIX.length()).trim();
        try {
            JWTClaimsSet claims = jwtManager.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            LoginUser loginUser = loginUserLoader.loadById(userId);
            if (loginUser == null) {
                throw new BizException(ResultCode.UNAUTHORIZED);
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BizException ex) {
            SecurityContextHolder.clearContext();
            SecurityExceptionHandler.writeBiz(response, jsonMapper, ex);
        } catch (NumberFormatException ex) {
            SecurityContextHolder.clearContext();
            SecurityExceptionHandler.writeBiz(response, jsonMapper, new BizException(ResultCode.UNAUTHORIZED));
        }
    }
}
