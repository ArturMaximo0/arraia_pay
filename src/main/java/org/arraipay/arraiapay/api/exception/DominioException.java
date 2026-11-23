package org.arraipay.arraiapay.api.exception;

public class DominioException extends RuntimeException {
    private final String codigo;

    public DominioException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() { return codigo; }
}
