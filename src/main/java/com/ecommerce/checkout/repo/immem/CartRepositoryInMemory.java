package com.ecommerce.checkout.repo.immem;

import com.ecommerce.checkout.model.CartDocument;
import com.ecommerce.checkout.repo.CartRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "inmem")
public class CartRepositoryInMemory implements CartRepository {

    private final Map<String, CartDocument> byId = new ConcurrentHashMap<>();
    private final Map<String, String> byUser = new ConcurrentHashMap<>();
    private final Map<String, String> byAnon = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    @Override
    public <S extends CartDocument> S save(S entity) {
        if (entity.id == null || entity.id.isBlank()) {
            entity.id = "c-" + seq.incrementAndGet();
            entity.createdAt = Instant.now();
        }
        entity.updatedAt = Instant.now();
        byId.put(entity.id, entity);
        if (entity.ownerUserId != null)
            byUser.put(entity.ownerUserId, entity.id);
        if (entity.ownerAnonId != null)
            byAnon.put(entity.ownerAnonId, entity.id);
        return entity;
    }

    public Optional<CartDocument> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<CartDocument> findByOwnerUserId(String ownerUserId) {
        var id = byUser.get(ownerUserId);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<CartDocument> findByOwnerAnonId(String ownerAnonId) {
        var id = byAnon.get(ownerAnonId);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }


    public List<CartDocument> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public void delete(CartDocument entity) {
        if (entity == null) return;
        byId.remove(entity.id);
        if (entity.ownerUserId != null) byUser.remove(entity.ownerUserId);
        if (entity.ownerAnonId != null) byAnon.remove(entity.ownerAnonId);
    }

    // --- unimplemented convenience methods from MongoRepository default ---

    public boolean existsById(String id) {
        return byId.containsKey(id);
    }


    public long count() {
        return byId.size();
    }


    public void deleteById(String id) {
        findById(id).ifPresent(this::delete);
    }


    public void deleteAll() {
        byId.clear();
        byUser.clear();
        byAnon.clear();
    }


    public <S extends CartDocument> List<S> saveAll(Iterable<S> entities) {
        List<S> out = new ArrayList<>();
        for (S e : entities) out.add(save(e));
        return out;
    }

    public List<CartDocument> findAllById(Iterable<String> ids) {
        List<CartDocument> out = new ArrayList<>();
        for (String id : ids) findById(id).ifPresent(out::add);
        return out;
    }


    public void deleteAll(Iterable<? extends CartDocument> entities) {
        entities.forEach(this::delete);
    }
}

