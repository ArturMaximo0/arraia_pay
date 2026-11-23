package org.arraipay.arraiapay.security;

import org.arraipay.arraiapay.repository.UsuarioRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthTokenService extends OncePerRequestFilter {
    private final UsuarioRepository usuarioRepository;
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    public AuthTokenService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public String criar(String login) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, login);
        return token;
    }

    public void remover(String token) {
        if (token != null) tokens.remove(token);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String login = tokens.get(authorization.substring(7));
            if (login != null) {
                usuarioRepository.findByLogin(login).ifPresent(usuario -> {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            usuario.getLogin(), null,
                            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }
        filterChain.doFilter(request, response);
    }
}
