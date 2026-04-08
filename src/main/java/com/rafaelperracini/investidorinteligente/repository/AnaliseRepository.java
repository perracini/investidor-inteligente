package com.rafaelperracini.investidorinteligente.repository;

import com.rafaelperracini.investidorinteligente.entity.AnaliseEntity;
import com.rafaelperracini.investidorinteligente.entity.TipoRecomendacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnaliseRepository extends JpaRepository<AnaliseEntity, Long> {

    Page<AnaliseEntity> findByTickerIgnoreCase(String ticker, Pageable pageable);

    Page<AnaliseEntity> findByRecomendacao(TipoRecomendacao recomendacao, Pageable pageable);

    Optional<AnaliseEntity> findTopByTickerIgnoreCaseOrderByCriadoEmDesc(String ticker);
}
