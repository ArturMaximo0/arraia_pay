package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.domain.Usuario;
import org.arraipay.arraiapay.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioAtual {
    private final UsuarioRepository usuarioRepository;

    public UsuarioAtual(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new IllegalStateException("Usuario nao autenticado");
        return usuarioRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuario inexistente"));
    }
}
