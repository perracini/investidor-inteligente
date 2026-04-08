package com.rafaelperracini.investidorinteligente.service;

import com.rafaelperracini.investidorinteligente.dto.AnaliseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnaliseService {

    AnaliseResponse analisar(String ticker);

    AnaliseResponse buscarPorId(Long id);

    Page<AnaliseResponse> listarPorTicker(String ticker, Pageable pageable);

    Page<AnaliseResponse> listarTodas(Pageable pageable);
}
