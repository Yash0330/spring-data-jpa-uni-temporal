package dev.yash.jpatemporal.repository;

import dev.yash.jpatemporal.domain.Temporal;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing entities with temporal boundaries.
 * <p>
 * This interface extends both {@link JpaRepository} and {@link TemporalCustomRepository},
 * providing a combination of standard JPA repository methods and custom methods for
 * handling entities with temporal boundaries.
 * </p>
 *
 * @param <T>  the type of the entity managed by this repository, which must extend {@link Temporal}
 * @param <ID> the type of the entity's identifier
 * @author Yashwanth M
 */
public interface TemporalRepository<T extends Temporal, ID>
        extends JpaRepository<T, ID>, TemporalCustomRepository<T, ID> {
}

