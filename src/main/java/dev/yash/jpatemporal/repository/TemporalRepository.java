package dev.yash.jpatemporal.repository;

import dev.yash.jpatemporal.domain.Temporal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemporalRepository<T extends Temporal, ID>
        extends JpaRepository<T, ID>, TemporalCustomRepository<T, ID> {
}

