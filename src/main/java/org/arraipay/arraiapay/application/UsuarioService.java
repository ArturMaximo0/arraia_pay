package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.Barraca;
import org.arraipay.arraiapay.domain.Usuario;
import org.arraipay.arraiapay.domain.enums.PerfilUsuario;
import org.arraipay.arraiapay.repository.BarracaRepository;
import org.arraipay.arraiapay.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final BarracaRepository barracaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, BarracaRepository barracaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.barracaRepository = barracaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario criar(String nome, String login, String senha, String perfil, Long idBarraca) {
        if (login == null || login.isBlank() || senha == null || senha.isBlank()) throw new DominioException("E21", "Login e senha obrigatorios.");
        if (usuarioRepository.findByLogin(login).isPresent()) throw new DominioException("E21", "Login ja cadastrado.");
        PerfilUsuario perfilUsuario;
        try { perfilUsuario = PerfilUsuario.valueOf(perfil.toUpperCase()); }
        catch (Exception exception) { throw new DominioException("E21", "Perfil invalido."); }

        Usuario usuario = new Usuario(nome, login, passwordEncoder.encode(senha), perfilUsuario);
        if (perfilUsuario == PerfilUsuario.BARRACA) {
            if (idBarraca == null) throw new DominioException("E21", "Usuario de barraca deve possuir barraca.");
            Barraca barraca = barracaRepository.findById(idBarraca)
                    .orElseThrow(() -> new DominioException("E08", "Barraca nao encontrada."));
            usuario.setBarraca(barraca);
        }
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listar(Boolean ativo, String perfil) {
        return usuarioRepository.findAll().stream()
                .filter(u -> ativo == null || u.isAtivo() == ativo)
                .filter(u -> perfil == null || u.getPerfil().name().equalsIgnoreCase(perfil))
                .toList();
    }

    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new DominioException("E21", "Usuario nao encontrado."));
    }

    @Transactional
    public Usuario atualizar(Long id, String nome, String perfil, Long idBarraca, Boolean ativo) {
        Usuario usuario = buscar(id);
        if (nome != null) usuario.setNome(nome);
        if (ativo != null) usuario.setAtivo(ativo);
        if (perfil != null) {
            PerfilUsuario perfilUsuario;
            try { perfilUsuario = PerfilUsuario.valueOf(perfil.toUpperCase()); }
            catch (Exception exception) { throw new DominioException("E21", "Perfil invalido."); }
            usuario.setPerfil(perfilUsuario);
            if (perfilUsuario == PerfilUsuario.BARRACA) {
                if (idBarraca == null) throw new DominioException("E21", "Usuario de barraca deve possuir barraca.");
                usuario.setBarraca(barracaRepository.findById(idBarraca)
                        .orElseThrow(() -> new DominioException("E08", "Barraca nao encontrada.")));
            } else {
                usuario.setBarraca(null);
            }
        }
        return usuarioRepository.save(usuario);
    }
}
