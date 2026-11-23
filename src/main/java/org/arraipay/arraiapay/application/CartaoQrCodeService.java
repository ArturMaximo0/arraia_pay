package org.arraipay.arraiapay.application;

import org.arraipay.arraiapay.api.exception.DominioException;
import org.arraipay.arraiapay.domain.CartaoQrCode;
import org.arraipay.arraiapay.repository.CartaoRepository;
import org.arraipay.arraiapay.repository.CartaoQrCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class CartaoQrCodeService {
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };

    private final CartaoRepository cartaoRepository;
    private final CartaoQrCodeRepository qrCodeRepository;
    private final HttpClient httpClient;

    public CartaoQrCodeService(CartaoRepository cartaoRepository, CartaoQrCodeRepository qrCodeRepository) {
        this.cartaoRepository = cartaoRepository;
        this.qrCodeRepository = qrCodeRepository;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Transactional
    public byte[] buscarOuGerar(Long idCartao) {
        var cartao = cartaoRepository.findByIdForUpdate(idCartao)
                .orElseThrow(() -> new DominioException("E16", "Cartao nao encontrado."));
        CartaoQrCode existente = qrCodeRepository.findById(idCartao).orElse(null);
        if (existente != null) return existente.getImagemPng();

        String conteudo = URLEncoder.encode(cartao.getCodigoQr(), StandardCharsets.UTF_8);
        URI uri = URI.create("https://qrgenapp.com/api/qr?data=" + conteudo + "&format=png&size=512&ec=H");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "image/png")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] imagem = response.body();
            if (response.statusCode() != 200 || !isPng(imagem)) {
                throw new DominioException("E22", "O servico de QR Code nao retornou uma imagem PNG valida.");
            }
            return qrCodeRepository.save(new CartaoQrCode(cartao, imagem)).getImagemPng();
        } catch (IOException exception) {
            throw new DominioException("E22", "Nao foi possivel consultar o servico de QR Code.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DominioException("E22", "A geracao do QR Code foi interrompida.");
        }
    }

    private boolean isPng(byte[] bytes) {
        if (bytes == null || bytes.length < PNG_SIGNATURE.length) return false;
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return true;
    }
}
