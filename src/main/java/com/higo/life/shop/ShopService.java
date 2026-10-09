package com.higo.life.shop;

import com.higo.life.support.NotFoundException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {

    private final ShopRepository shopRepository;

    public ShopService(ShopRepository shopRepository) {
        this.shopRepository = shopRepository;
    }

    @Transactional
    public Shop create(String name, String category, String address) {
        return shopRepository.save(new Shop(name, category, address));
    }

    @Transactional(readOnly = true)
    public Shop get(Long id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商家不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<Shop> list() {
        return shopRepository.findAll(Sort.by("id").ascending());
    }
}

