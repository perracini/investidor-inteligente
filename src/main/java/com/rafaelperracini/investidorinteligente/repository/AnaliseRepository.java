package com.rafaelperracini.investidorinteligente.repository;

import com.rafaelperracini.investidorinteligente.entity.AnaliseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AnaliseRepository
        extends JpaRepository<AnaliseEntity, Long>, JpaSpecificationExecutor<AnaliseEntity> {

    Optional<AnaliseEntity> findTopByTickerIgnoreCaseOrderByCriadoEmDesc(String ticker);
}
