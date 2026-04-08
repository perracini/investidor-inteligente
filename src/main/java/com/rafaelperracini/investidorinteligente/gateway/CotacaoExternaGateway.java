package com.rafaelperracini.investidorinteligente.gateway;

import com.rafaelperracini.investidorinteligente.dto.CotacaoResumo;

import java.util.Optional;

public interface CotacaoExternaGateway {

    Optional<CotacaoResumo> buscarCotacao(String ticker);
}
