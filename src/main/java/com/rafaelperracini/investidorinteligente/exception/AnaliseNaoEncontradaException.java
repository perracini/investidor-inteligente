package com.rafaelperracini.investidorinteligente.exception;

public class AnaliseNaoEncontradaException extends RuntimeException {

    public AnaliseNaoEncontradaException(Long id) {
        super("Analise nao encontrada: " + id);
    }
}
