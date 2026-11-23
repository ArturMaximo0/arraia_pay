package org.arraipay.arraiapay.api.controller;

import org.arraipay.arraiapay.application.UsuarioService;
import org.arraipay.arraiapay.api.dto.Requests.UsuarioRequest;
import org.arraipay.arraiapay.api.dto.Requests.UsuarioUpdateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;
    public UsuarioController(UsuarioService usuarioService) { this.usuarioService = usuarioService; }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object criar(@RequestBody UsuarioRequest request) {
        return usuarioService.criar(request.nome(), request.login(), request.senha(), request.perfil(), request.idBarraca());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object listar(@RequestParam(name = "ativo", required = false) Boolean ativo,
                         @RequestParam(name = "perfil", required = false) String perfil,
                         @RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "size", defaultValue = "20") int size,
                         @RequestParam(name = "sort", defaultValue = "idUsuario,asc") String sort) {
        return usuarioService.listar(ativo, perfil);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object buscar(@PathVariable("id") Long id) {
        return usuarioService.buscar(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object atualizar(@PathVariable("id") Long id, @RequestBody UsuarioUpdateRequest request) {
        return usuarioService.atualizar(id, request.nome(), request.perfil(), request.idBarraca(), request.ativo());
    }
}
