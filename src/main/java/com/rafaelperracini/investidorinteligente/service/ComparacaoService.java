package com.rafaelperracini.investidorinteligente.service;

import com.rafaelperracini.investidorinteligente.dto.ComparacaoResponse;

import java.util.List;

public interface ComparacaoService {

    ComparacaoResponse comparar(List<String> tickers);
}
