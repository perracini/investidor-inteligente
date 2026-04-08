package com.rafaelperracini.investidorinteligente.exception;

public class CotacaoIndisponivelException extends RuntimeException {

    public CotacaoIndisponivelException(String ticker) {
        super("Nao foi possivel obter cotacao para " + ticker.toUpperCase() +
              ". Verifique se o ticker e valido ou tente novamente mais tarde.");
    }
}
