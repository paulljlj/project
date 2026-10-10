package com.higo.life.shop;

import com.higo.life.cache.ShopCache;
import com.higo.life.support.InvalidRequestException;
import com.higo.life.support.NotFoundException;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final ObjectProvider<ShopCache> shopCache;
    private final ObjectProvider<ShopGeoService> shopGeoService;

    public ShopService(
            ShopRepository shopRepository,
            ObjectProvider<ShopCache> shopCache,
            ObjectProvider<ShopGeoService> shopGeoService
    ) {
        this.shopRepository = shopRepository;
        this.shopCache = shopCache;
        this.shopGeoService = shopGeoService;
    }

    @Transactional
    public Shop create(String name, String category, String address, Double longitude, Double latitude) {
        validateCoordinates(longitude,latitude);
        Shop shop = shopRepository.save(new Shop(name, category, address, longitude, latitude));
        ShopGeoService geo = shopGeoService.getIfAvailable();
        if (geo != null) {
            com.higo.life.support.AfterCommit.run(() -> geo.index(shop));
        }
        return shop;
    }

    @Transactional(readOnly = true)
    public ShopResponse get(Long id) {
        ShopCache cache = shopCache.getIfAvailable();
        Optional<ShopResponse> result = cache == null
                ? loadResponse(id)
                : cache.get(id, () -> loadResponse(id));
        return result
                .orElseThrow(() -> new NotFoundException("商家不存在: " + id));
    }

    private void validateCoordinates(Double longitude,Double latitude) {
        if((longitude==null)!=(latitude==null)) throw new InvalidRequestException("经纬度必须同时提供");
    }
    public List<String> categories() { return shopRepository.categories(); }
    public List<ShopResponse> byName(String name, int page) {
        if(page<0 || page>1000) throw new InvalidRequestException("页码不合法");
        return shopRepository.findByNameContainingIgnoreCaseOrderByIdAsc(name,PageRequest.of(page,10)).stream().map(ShopResponse::from).toList();
    }
    public ShopResponse logical(Long id) {
        ShopCache cache=shopCache.getIfAvailable();
        return cache==null ? get(id) : cache.logical(id,()->loadResponse(id)).orElseThrow(()->new NotFoundException("商家不存在"));
    }
    private Optional<ShopResponse> loadResponse(Long id) {
        return shopRepository.findById(id).map(ShopResponse::from);
    }

    @Transactional
    public ShopResponse update(Long id, UpdateShopRequest request) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商家不存在: " + id));
        validateCoordinates(request.longitude(),request.latitude());
        String oldCategory = shop.getCategory();
        shop.update(request.name(), request.category(), request.address(), request.longitude(), request.latitude());
        shopRepository.flush();

        ShopCache cache = shopCache.getIfAvailable();
        if (cache != null) {
            com.higo.life.support.AfterCommit.run(() -> cache.evict(id));
        }
        ShopGeoService geo = shopGeoService.getIfAvailable();
        if (geo != null) {
            com.higo.life.support.AfterCommit.run(() -> { geo.remove(oldCategory, id); geo.index(shop); });
        }
        return ShopResponse.from(shop);
    }

    @Transactional(readOnly = true)
    public List<Shop> list() {
        return shopRepository.findAll(Sort.by("id").ascending());
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> search(
            String category,
            int page,
            Double longitude,
            Double latitude
    ) {
        if (page < 0 || page > 1000) throw new InvalidRequestException("页码需在 0 到 1000 之间");
        if ((longitude == null) != (latitude == null)) throw new InvalidRequestException("经纬度必须同时提供");
        if (longitude != null && (!Double.isFinite(longitude) || !Double.isFinite(latitude) || longitude < -180 || longitude > 180 || latitude < -85.05112878 || latitude > 85.05112878)) throw new InvalidRequestException("坐标超出 Redis GEO 范围");
        int safePage = page;
        if (longitude == null || latitude == null) {
            return shopRepository.findByCategoryOrderByIdAsc(category, PageRequest.of(safePage, 10))
                    .stream().map(ShopResponse::from).toList();
        }
        ShopGeoService geo = shopGeoService.getIfAvailable();
        if (geo == null) {
            throw new InvalidRequestException("GEO 查询需要启用 Redis");
        }
        int end = (safePage + 1) * 10;
        Map<Long, Double> candidates = geo.nearby(category, longitude, latitude, end);
        List<Long> pageIds = candidates.keySet().stream().skip((long) safePage * 10).limit(10).toList();
        Map<Long, Shop> shops = shopRepository.findAllById(pageIds).stream()
                .collect(java.util.stream.Collectors.toMap(Shop::getId, value -> value));
        return pageIds.stream()
                .filter(shops::containsKey)
                .map(id -> ShopResponse.from(shops.get(id)).withDistance(candidates.get(id)))
                .toList();
    }
}

