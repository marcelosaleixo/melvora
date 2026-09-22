package com.marceloaleixo.melvora.repository;

import com.marceloaleixo.melvora.entity.ProdutoMegaHair;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProdutoMegaHairRepository extends JpaRepository<ProdutoMegaHair, Long> {
    Optional<ProdutoMegaHair> findByProdutoId(Long produtoId);}
