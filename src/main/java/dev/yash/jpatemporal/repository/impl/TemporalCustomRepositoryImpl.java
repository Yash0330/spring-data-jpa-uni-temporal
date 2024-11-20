package dev.yash.jpatemporal.repository.impl;

import dev.yash.jpatemporal.repository.TemporalCustomRepository;
import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import dev.yash.jpatemporal.domain.Temporal;

/**
 * Custom repository implementation for handling temporal entities.
 * <p>
 * This class provides custom implementations of {@link TemporalCustomRepository},
 * adding logic for filtering entities where {@code timeOut} equals {@link Temporal#INFINITY}.
 * </p>
 *
 * @param <T>  the type of the entity extending {@code Temporal}
 * @param <ID> the type of the primary key for the entity
 * @author Yashwanth M
 */
@Repository
public class TemporalCustomRepositoryImpl<T, ID> implements TemporalCustomRepository<T, ID> {

    @PersistenceContext
    private EntityManager entityManager;

    private final Class<T> domainClass;

    /**
     * Constructor to initialize the repository with the specific entity class.
     *
     * @param domainClass the entity class for which this repository is created
     */
    public TemporalCustomRepositoryImpl(Class<T> domainClass) {
        this.domainClass = domainClass;
    }

    /**
     * Retrieves all entities of the specified type where {@code timeOut} is equal to {@link Temporal#INFINITY}.
     *
     * @return a list of all entities with {@code timeOut} set to {@link Temporal#INFINITY}
     */
    @Override
    @Nonnull
    public List<T> findAll() {
        return entityManager.createQuery(
                        "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE e." + Temporal.TIME_OUT_FIELD + " = :timeOut", domainClass)
                .setParameter("timeOut", Temporal.INFINITY)
                .getResultList();
    }

    /**
     * Retrieves all entities by their primary keys where {@code timeOut} is equal to {@link Temporal#INFINITY}.
     *
     * @param ids the list of primary keys to retrieve
     * @return a list of entities matching the given IDs and having {@code timeOut} set to {@link Temporal#INFINITY}
     */
    @Override
    @Nonnull
    public Iterable<T> findAllById(Iterable<ID> ids) {
        return entityManager.createQuery(
                        "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE e.id IN :ids AND e." + Temporal.TIME_OUT_FIELD + " = :timeOut", domainClass)
                .setParameter("ids", ids)
                .setParameter("timeOut", Temporal.INFINITY)
                .getResultList();
    }

    /**
     * @return
     */
    @Override
    public long count() {
        return entityManager.createQuery(
                        "SELECT COUNT(e) FROM " + domainClass.getSimpleName() + " e WHERE e." + Temporal.TIME_OUT_FIELD + " = :timeOut", Long.class)
                .setParameter("timeOut", Temporal.INFINITY)
                .getSingleResult();
    }

    /**
     * @param id
     */
    @Override
    public void deleteById(ID id) {

    }

    /**
     * @param entity
     */
    @Override
    public void delete(T entity) {

    }

    /**
     * @param ids
     */
    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {

    }

    /**
     * @param entities
     */
    @Override
    public void deleteAll(Iterable<? extends T> entities) {

    }

    /**
     *
     */
    @Override
    public void deleteAll() {

    }

    /**
     * Retrieves all entities of the specified type in batches, where {@code timeOut} is equal to {@link Temporal#INFINITY}.
     * <p>
     * This method fetches entities in batches to avoid memory overload when dealing with large datasets.
     * The {@code fetchSize} is set to the provided {@code batchSize} to control how many rows are retrieved in a single database call.
     * </p>
     *
     * @param batchSize the number of entities to retrieve per batch
     * @return a list of entities with {@code timeOut} set to {@link Temporal#INFINITY}
     */
    @Override
    @Nonnull
    public List<T> findAllByBatchSize(int batchSize) {
        return entityManager.createQuery(
                        "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE e." + Temporal.TIME_OUT_FIELD + " = :timeOut", domainClass)
                .setParameter("timeOut", Temporal.INFINITY)
                .setHint("org.hibernate.fetchSize", batchSize)  // Set the fetch size hint to the batch size
                .getResultList();
    }


    /**
     * Retrieves an entity by its primary key only if its {@code timeOut} equals {@link Temporal#INFINITY}.
     *
     * @param id the primary key of the entity to retrieve
     * @return an {@code Optional} containing the entity if found and its {@code timeOut} is {@link Temporal#INFINITY}, otherwise empty
     */
    @Override
    @Nonnull
    public Optional<T> findById(ID id) {
        T entity = entityManager.createQuery(
                        "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE e.id = :id AND e." + Temporal.TIME_OUT_FIELD + " = :timeOut", domainClass)
                .setParameter("id", id)
                .setParameter("timeOut", Temporal.INFINITY)
                .getResultStream()
                .findFirst()
                .orElse(null);
        return Optional.ofNullable(entity);
    }

    /**
     * Checks if an entity with the given ID exists and is active (i.e., its {@code timeOut} is equal to {@link Temporal#INFINITY}).
     * <p>
     * This method checks if an entity with the provided ID exists in the database and its {@code timeOut} field
     * is set to {@link Temporal#INFINITY}. This indicates that the entity is considered active.
     * </p>
     *
     * @param id the ID of the entity to check for existence
     * @return {@code true} if the entity exists and its {@code timeOut} is equal to {@link Temporal#INFINITY}, otherwise {@code false}
     */
    @Override
    public boolean existsById(ID id) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(e) FROM " + domainClass.getSimpleName() + " e WHERE e.id = :id AND e." + Temporal.TIME_OUT_FIELD + " = :timeOut", Long.class)
                .setParameter("id", id)
                .setParameter("timeOut", Temporal.INFINITY)
                .getSingleResult();
        return count > 0;
    }



    /**
     * Finds a single entity by applying a custom predicate and ensuring {@code timeOut} is equal to {@link Temporal#INFINITY}.
     *
     * @param predicate the custom predicate for filtering entities
     * @return an {@code Optional} containing the entity if found, otherwise empty
     */
    @Override
    @Nonnull
    public Optional<T> find(String predicate) {
        T entity = entityManager.createQuery(
                        "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE " + predicate + " AND e." + Temporal.TIME_OUT_FIELD + " = :timeOut", domainClass)
                .setParameter("timeOut", Temporal.INFINITY)
                .getResultStream()
                .findFirst()
                .orElse(null);
        return Optional.ofNullable(entity);
    }

    /**
     * Saves a single entity to the database.
     * <p>
     * If the entity already exists, its {@code timeOut} is updated to the current time in milliseconds,
     * and a new entity is saved with {@code timeIn} set to the current time and {@code timeOut} set to {@link Temporal#INFINITY}.
     * </p>
     *
     * @param entity the entity to save
     * @return the new saved entity with {@code timeOut} set to {@link Temporal#INFINITY}
     */
    @Override
    @Transactional
    @Nonnull
    public <S extends T> S save(S entity) {
        // Get the current time in milliseconds
        long currentTimeMillis = System.currentTimeMillis();

        // Get the ID of the entity
        ID entityId = (ID) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);

        // Check if the entity exists
        Optional<T> existingEntityOpt = findById(entityId);

        if (existingEntityOpt.isPresent()) {
            // Update the `timeOut` field of the existing entity
            T existingEntity = existingEntityOpt.get();
            try {
                // Use reflection to set the `timeOut` field
                existingEntity.getClass().getDeclaredField(Temporal.TIME_OUT_FIELD).setAccessible(true);
                existingEntity.getClass().getDeclaredField(Temporal.TIME_OUT_FIELD).set(existingEntity, currentTimeMillis);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to update the timeOut field for the existing entity", e);
            }
            entityManager.merge(existingEntity);
        }

        // Set the `timeIn` and `timeOut` fields for the new entity
        try {
            // Use reflection to set the `timeIn` field
            entity.getClass().getDeclaredField(Temporal.TIME_IN_FIELD).setAccessible(true);
            entity.getClass().getDeclaredField(Temporal.TIME_IN_FIELD).set(entity, currentTimeMillis);

            // Use reflection to set the `timeOut` field
            entity.getClass().getDeclaredField(Temporal.TIME_OUT_FIELD).setAccessible(true);
            entity.getClass().getDeclaredField(Temporal.TIME_OUT_FIELD).set(entity, Temporal.INFINITY);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set the timeIn or timeOut field for the new entity", e);
        }

        // Persist the new entity
        entityManager.persist(entity);

        return entity;
    }

    /**
     * Saves multiple entities to the database.
     * Each entity is either persisted if new or merged if already existing.
     *
     * @param entities the list of entities to save
     * @return the list of saved entities
     */
    @Override
    @Nonnull
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        for (T entity : entities) {
            save(entity);
        }
        return entities;
    }
}



