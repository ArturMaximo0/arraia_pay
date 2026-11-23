package org.arraipay.arraiapay.config;

import org.arraipay.arraiapay.domain.Barraca;
import org.arraipay.arraiapay.domain.Carteira;
import org.arraipay.arraiapay.domain.Cartao;
import org.arraipay.arraiapay.domain.Cliente;
import org.arraipay.arraiapay.domain.Usuario;
import org.arraipay.arraiapay.domain.enums.PerfilUsuario;
import org.arraipay.arraiapay.repository.BarracaRepository;
import org.arraipay.arraiapay.repository.CarteiraRepository;
import org.arraipay.arraiapay.repository.CartaoRepository;
import org.arraipay.arraiapay.repository.ClienteRepository;
import org.arraipay.arraiapay.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev-h2")
public class DevDataInitializer implements CommandLineRunner {
    private final UsuarioRepository usuarioRepository;
    private final BarracaRepository barracaRepository;
    private final ClienteRepository clienteRepository;
    private final CarteiraRepository carteiraRepository;
    private final CartaoRepository cartaoRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataInitializer(UsuarioRepository usuarioRepository,
                              BarracaRepository barracaRepository,
                              ClienteRepository clienteRepository,
                              CarteiraRepository carteiraRepository,
                              CartaoRepository cartaoRepository,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.barracaRepository = barracaRepository;
        this.clienteRepository = clienteRepository;
        this.carteiraRepository = carteiraRepository;
        this.cartaoRepository = cartaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Barraca barraca = barracaRepository.findAll().stream()
                .filter(item -> item.getNome().equals("Barraca Central"))
                .findFirst()
                .orElseGet(() -> barracaRepository.save(new Barraca("Barraca Central", "Responsavel da Barraca")));

        criarUsuario("Administrador", "admin", "admin123", PerfilUsuario.ADMINISTRADOR, null);
        criarUsuario("Caixa", "caixa", "caixa123", PerfilUsuario.CAIXA, null);
        criarUsuario("Operador da Barraca", "barraca", "barraca123", PerfilUsuario.BARRACA, barraca);

        Cliente cliente = clienteRepository.findAll().stream()
                .filter(item -> item.getNome().equals("Cliente Demonstracao"))
                .findFirst()
                .orElseGet(() -> {
                    Cliente novo = clienteRepository.save(new Cliente("Cliente Demonstracao"));
                    novo.setDocumento("123.456.789-09");
                    novo.setTelefone("(11) 99999-9999");
                    novo = clienteRepository.save(novo);
                    Carteira carteira = carteiraRepository.save(new Carteira(novo));
                    novo.setCarteira(carteira);
                    return novo;
                });

        if (cliente.getCarteira() == null) {
            cliente.setCarteira(carteiraRepository.save(new Carteira(cliente)));
        }

        boolean cartaoDemoExiste = cartaoRepository.findAll().stream()
                .anyMatch(cartao -> cartao.getCliente().getIdCliente().equals(cliente.getIdCliente()));
        if (!cartaoDemoExiste) {
            cartaoRepository.save(new Cartao(cliente));
        }

    }

    private void criarUsuario(String nome, String login, String senha,
                              PerfilUsuario perfil, Barraca barraca) {
        if (usuarioRepository.findByLogin(login).isPresent()) return;
        Usuario usuario = new Usuario(nome, login, passwordEncoder.encode(senha), perfil);
        usuario.setBarraca(barraca);
        usuarioRepository.save(usuario);
    }
}
