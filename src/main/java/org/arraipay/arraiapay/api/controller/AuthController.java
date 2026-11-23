package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.api.dto.Requests.LoginRequest;
import org.arraipay.arraiapay.domain.Usuario;
import org.arraipay.arraiapay.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;
import org.arraipay.arraiapay.security.AuthTokenService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final AuthTokenService authTokenService;

    public AuthController(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository,
                          AuthTokenService authTokenService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/login")
    public Object login(@RequestBody LoginRequest request,
                        HttpServletRequest httpRequest,
                        HttpServletResponse httpResponse) {
        SecurityContextHolder.clearContext();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.senha()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Usuario usuario = usuarioRepository.findByLogin(request.login()).orElseThrow();
        String token = authTokenService.criar(request.login());
        return java.util.Map.of(
                "id", usuario.getIdUsuario(),
                "nome", usuario.getNome(),
                "perfil", usuario.getPerfil().name(),
                "barracaId", usuario.getBarraca() == null ? 0 : usuario.getBarraca().getIdBarraca(),
                "token", token);
    }

    @PostMapping("/logout")
    public Object logout(jakarta.servlet.http.HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authTokenService.remover(authorization.substring(7));
        }
        SecurityContextHolder.clearContext();
        return java.util.Map.of("message", "Logout realizado.");
    }

    @GetMapping("/me")
    public Object usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Sessao ausente");
        }
        Usuario usuario = usuarioRepository.findByLogin(authentication.getName()).orElseThrow();
        return java.util.Map.of(
                "id", usuario.getIdUsuario(),
                "nome", usuario.getNome(),
                "perfil", usuario.getPerfil().name(),
                "barracaId", usuario.getBarraca() == null ? 0 : usuario.getBarraca().getIdBarraca());
    }
}
