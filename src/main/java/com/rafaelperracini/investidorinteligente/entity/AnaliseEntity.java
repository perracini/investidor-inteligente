package com.rafaelperracini.investidorinteligente.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "analises")
public class AnaliseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false)
    private String empresa;

    @Column(nullable = false)
    private double preco;

    private double variacao;

    private double dividendYield;

    private double pl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRecomendacao recomendacao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String parecer;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    public AnaliseEntity() {
    }

    public AnaliseEntity(String ticker, String empresa, double preco, double variacao,
                         double dividendYield, double pl, TipoRecomendacao recomendacao,
                         String parecer, LocalDateTime criadoEm) {
        this.ticker = ticker;
        this.empresa = empresa;
        this.preco = preco;
        this.variacao = variacao;
        this.dividendYield = dividendYield;
        this.pl = pl;
        this.recomendacao = recomendacao;
        this.parecer = parecer;
        this.criadoEm = criadoEm;
    }

    public Long getId() { return id; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }

    public double getPreco() { return preco; }
    public void setPreco(double preco) { this.preco = preco; }

    public double getVariacao() { return variacao; }
    public void setVariacao(double variacao) { this.variacao = variacao; }

    public double getDividendYield() { return dividendYield; }
    public void setDividendYield(double dividendYield) { this.dividendYield = dividendYield; }

    public double getPl() { return pl; }
    public void setPl(double pl) { this.pl = pl; }

    public TipoRecomendacao getRecomendacao() { return recomendacao; }
    public void setRecomendacao(TipoRecomendacao recomendacao) { this.recomendacao = recomendacao; }

    public String getParecer() { return parecer; }
    public void setParecer(String parecer) { this.parecer = parecer; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
