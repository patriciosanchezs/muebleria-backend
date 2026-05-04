package com.muebleria.repository;

import com.muebleria.model.PushSubscription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PushSubscriptionRepository extends MongoRepository<PushSubscription, String> {

    List<PushSubscription> findByUsername(String username);

    void deleteByEndpoint(String endpoint);

    boolean existsByUsernameAndEndpoint(String username, String endpoint);
}
