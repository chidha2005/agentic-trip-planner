package com.de.app.agentic.repository;

import com.de.app.agentic.entity.TripRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TripRequestRepository extends JpaRepository<TripRequestEntity, UUID> {
}