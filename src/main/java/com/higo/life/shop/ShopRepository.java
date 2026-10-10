package com.higo.life.shop;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    @org.springframework.data.jpa.repository.Query("select distinct s.category from Shop s order by s.category")
    List<String> categories();
    List<Shop> findByNameContainingIgnoreCaseOrderByIdAsc(String name, Pageable page);
    List<Shop> findByCategoryOrderByIdAsc(String category, Pageable pageable);
}

