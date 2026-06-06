package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class Filter extends OncePerRequestFilter {

    private static final String DEV_AUTH_HEADER = "X-Dev-Auth-Id";

    @Autowired
    JwtVerification verification;

    @Value("${app.dev-auth-enabled:false}")
    private boolean devAuthEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (devAuthEnabled) {
            String devAuthId = request.getHeader(DEV_AUTH_HEADER);
            if (devAuthId != null && !devAuthId.isBlank()) {
                DemoUserDetails userDetails = new DemoUserDetails(devAuthId.trim());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authorizationHeader.substring(7);
        try{
            String authId = verification.getAuthId(token);
            DemoUserDetails userDetails = new DemoUserDetails(authId);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, token, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        catch (Exception e){
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().println(e.getMessage());
            return;
        }
        filterChain.doFilter(request, response);

    }

}
