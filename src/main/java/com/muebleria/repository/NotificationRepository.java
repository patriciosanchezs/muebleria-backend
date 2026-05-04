package com.muebleria.repository;

import com.muebleria.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findTop20ByUsernameOrderByCreatedAtDesc(String username);

    long countByUsernameAndReadFalse(String username);

    void deleteByUsername(String username);
}
