package com.lcorp.backend.repository;

import com.lcorp.backend.domain.TransportationRequestNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransportationRequestNoteRepository extends JpaRepository<TransportationRequestNote, UUID> {
    @EntityGraph(attributePaths = "author")
    List<TransportationRequestNote> findByRequest_IdOrderByPinnedDescCreatedAtDesc(Long requestId);

    @EntityGraph(attributePaths = "author")
    Optional<TransportationRequestNote> findByIdAndRequest_Id(UUID id, Long requestId);
}
