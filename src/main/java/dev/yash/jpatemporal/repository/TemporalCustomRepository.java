package dev.yash.jpatemporal.repository;

import dev.yash.jpatemporal.domain.Temporal;
import jakarta.annotation.Nonnull;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Custom repository interface for managing entities with temporal boundaries.
 * <p>
 * This interface provides additional methods for handling entities that have a temporal lifecycle
 * defined by {@code timeIn} and {@code timeOut} fields, supplementing the standard {@link CrudRepository} methods.
 * </p>
 *
 * @param <T>  the type of the entity managed by this repository
 * @param <ID> the type of the entity's identifier
 * @author Yashwanth M
 */
public interface TemporalCustomRepository<T extends Temporal, ID> extends CrudRepository<T, ID> {

    /**
     * Retrieves all active entities, where the {@code timeOut} field is set to {@code Temporal.INFINITY}.
     *
     * @return a list of active entities
     */
    @Override
    @Nonnull
    List<T> findAll();

    /**
     * Retrieves active entities in batches of the specified size, where the {@code timeOut} field is set
     * to {@code Temporal.INFINITY}.
     *
     * @param batchSize the maximum number of entities to retrieve in a single batch
     * @return a list of active entities
     */
    @Nonnull
    List<T> findAllByBatchSize(int batchSize);

    /**
     * Retrieves an active entity by its identifier, where the {@code timeOut} field is set
     * to {@code Temporal.INFINITY}.
     *
     * @param id the identifier of the entity
     * @return an {@link Optional} containing the active entity if found, or empty if not found
     */
    @Override
    @Nonnull
    Optional<T> findById(ID id);

    /**
     * Retrieves an active entity based on a custom predicate, where the {@code timeOut} field is set
     * to {@code Temporal.INFINITY}.
     *
     * @param predicate the custom predicate to filter entities
     * @return an {@link Optional} containing the active entity if found, or empty if not found
     */
    @Nonnull
    Optional<T> find(String predicate);

    void saveInBatch(List<T> entities);

    void saveInBatch(List<T> entities, int batchSize);

    void saveInBatch(List<T> entities, EntityManager entityManager);

    void deleteInBatch(List<T> entities);

    void deleteInBatch(List<T> entities, int batchSize);

    void deleteInBatch(List<T> entities, EntityManager entityManager);
}
