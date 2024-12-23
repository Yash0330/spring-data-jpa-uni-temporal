package dev.yash.jpatemporal.repository;

import dev.yash.jpatemporal.domain.Temporal;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

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
@NoRepositoryBean
public interface TemporalRepository<T extends Temporal, ID> extends CrudRepository<T, ID> {

    /**
     * Retrieves all active entities, where the {@code timeOut} field is set to {@code Temporal.INFINITY}.
     *
     * @return a list of active entities
     */
    @Nonnull
    List<T> findAll();

    /**
     * Retrieves an active entity by its identifier, where the {@code timeOut} field is set
     * to {@code Temporal.INFINITY}.
     *
     * @param id the identifier of the entity
     * @return an {@link Optional} containing the active entity if found, or empty if not found
     */
    @Nonnull
    Optional<T> findById(@Nonnull ID id);

    /**
     * Retrieves all entities by their primary keys where {@code timeOut} is equal to {@link Temporal#INFINITY}.
     *
     * @param ids the list of primary keys to retrieve
     * @return a list of entities matching the given IDs and having {@code timeOut} set to {@link Temporal#INFINITY}
     */
    @Nonnull
    List<T> findAllById(@Nonnull Iterable<ID> ids);

    /**
     * Performs a batch save operation on the provided list of entities.
     * <p>
     * This method updates the {@code timeOut} field of existing entities with the current time
     * in milliseconds and inserts new entities into the database as active entities with their
     * {@code timeOut} field set to {@code Temporal.INFINITY}.
     * The operation is executed in batches to optimize performance for large datasets.
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities the list of entities to save in batch
     */
    void saveInBatch(@Nonnull List<T> entities);

    /**
     * Checks if an entity with the given ID excluding timeIn exists and is active (i.e., its {@code timeOut} is equal to {@link Temporal#INFINITY}).
     * <p>
     * This method checks if an entity with the provided ID excluding timeIn exists in the database and its {@code timeOut} field
     * is set to {@link Temporal#INFINITY}. This indicates that the entity is considered active.
     * </p>
     *
     * @param id the ID of the entity excluding timeIn to check for existence
     * @return {@code true} if the entity exists and its {@code timeOut} is equal to {@link Temporal#INFINITY}, otherwise {@code false}
     */
    boolean existsByIdExcludingTimeIn(@Nonnull ID id);

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
     * Retrieves an active entity based on a custom predicate, where the {@code timeOut} field is set
     * to {@code Temporal.INFINITY}.
     *
     * @param predicate the custom predicate to filter entities
     * @return an {@link Optional} containing the active entity if found, or empty if not found
     */
    Optional<T> find(@Nonnull String predicate);


    /**
     * Performs a batch save operation on the provided list of entities with a specified batch size.
     * <p>
     * This method updates the {@code timeOut} field of existing entities with the current time in milliseconds
     * and inserts new entities as active, with their {@code timeOut} field set to {@code Temporal.INFINITY}.
     * </p>
     * <p>
     * The operation is executed in batches of the specified size to optimize performance for large datasets.
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities  the list of entities to save in batch
     * @param batchSize the size of each batch for processing
     * @throws IllegalArgumentException if {@code batchSize} is less than 1
     */
    void saveInBatch(@Nonnull List<T> entities, int batchSize);

    /**
     * Performs a batch save operation using the specified {@link EntityManager}.
     * <p>
     * This method updates the {@code timeOut} field of existing entities with the current time in milliseconds
     * and inserts new entities as active, with their {@code timeOut} field set to {@code Temporal.INFINITY}.
     * </p>
     * <p>
     * The operation is executed in batches of the default batch size {@code DEFAULT_BATCH_SIZE} to optimize performance.
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities the list of entities to save in batch
     * @param entityManager       the {@link EntityManager} to use for the operation
     * @throws IllegalArgumentException if the {@code EntityManager} is {@code null}
     */
    void saveInBatch(@Nonnull List<T> entities, EntityManager entityManager);

    /**
     * Performs a soft delete operation on the provided list of entities in batches.
     * <p>
     * This method updates the {@code timeOut} field of existing entities to the current time
     * in milliseconds to mark them as inactive (soft delete).
     * The operation is executed in batches to optimize performance for large datasets.
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities the list of entities to softly delete in batch
     */
    void deleteInBatch(@Nonnull List<T> entities);

    /**
     * Performs a soft delete operation on the provided list of entities in batches of a specified size.
     * <p>
     * This method updates the {@code timeOut} field of existing entities to the current time
     * in milliseconds to mark them as inactive (soft delete). The operation is executed in
     * batches of the given size to optimize performance for large datasets.
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities  the list of entities to softly delete in batch
     * @param batchSize the number of entities to process in each batch
     * @throws IllegalArgumentException if {@code batchSize} is less than 1
     */
    void deleteInBatch(@Nonnull List<T> entities, int batchSize);

    /**
     * Performs a soft delete operation on the provided list of entities using the given {@link EntityManager}.
     * <p>
     * This method updates the {@code timeOut} field of existing entities to the current time
     * in milliseconds to mark them as inactive (soft delete). The operation is executed in
     * batches of the default size to optimize performance for large datasets.
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities the list of entities to softly delete in batch
     * @param entityManager the {@link EntityManager} used to perform the operation
     * @throws IllegalArgumentException if the {@code EntityManager} is {@code null}
     */
    void deleteInBatch(@Nonnull List<T> entities, EntityManager entityManager);
}
