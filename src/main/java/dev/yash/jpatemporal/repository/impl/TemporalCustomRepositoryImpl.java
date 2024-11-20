package dev.yash.jpatemporal.repository.impl;

import dev.yash.jpatemporal.domain.Temporal;
import dev.yash.jpatemporal.repository.TemporalCustomRepository;
import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

/**
 * Custom repository implementation for handling temporal entities.
 * <p>
 * This class provides custom implementations of {@link TemporalCustomRepository},
 * adding logic for filtering entities where {@code timeOut} equals {@link Temporal#INFINITY}.
 * It also includes batch processing capabilities for operations on large datasets.
 * </p>
 *
 * <p><b>Batch Size:</b></p>
 * The default batch size for processing entities is defined by the property
 * {@code jpa.temporal.batchSize}. If not explicitly configured, it defaults to 700.
 * Batch processing is used in operations like {@code saveInBatch}, {@code deleteInBatch},
 * and similar methods to enhance performance when dealing with large collections.
 *
 * @param <T>  the type of the entity extending {@code Temporal}
 * @param <ID> the type of the primary key for the entity
 * @author Yashwanth M
 */

@Repository
public class TemporalCustomRepositoryImpl<T extends Temporal, ID> implements TemporalCustomRepository<T, ID> {

    private final Class<T> domainClass;
    @Value("${jpa.temporal.batchSize:700}")
    private int DEFAULT_BATCH_SIZE;
    @PersistenceContext
    private EntityManager entityManager;

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
     * Counts the total number of active entities in the database.
     * <p>
     * This method returns the count of all entities where the {@code timeOut} field is equal to {@link Temporal#INFINITY}.
     * Active entities are defined as those with {@code timeOut} set to an infinite value, indicating they are currently valid.
     * </p>
     *
     * @return the total number of active entities
     */
    @Override
    public long count() {
        return entityManager.createQuery(
                        "SELECT COUNT(e) FROM " + domainClass.getSimpleName() + " e WHERE e." + Temporal.TIME_OUT_FIELD + " = :timeOut", Long.class)
                .setParameter("timeOut", Temporal.INFINITY)
                .getSingleResult();
    }

    /**
     * Performs a soft delete on entity with the given ID by setting their {@code timeOut} field
     * to the current time in milliseconds.
     *
     * @param id the ID of the entities to softly delete
     */
    @Override
    public void deleteById(ID id) {
        long currentTimeMillis = System.currentTimeMillis();

        T entity = entityManager.find(domainClass, id);
        if (entity != null) {
            entity.setTimeOut(currentTimeMillis);
            entityManager.merge(entity);
        }
    }

    /**
     * Performs a soft delete on the given entity by setting its {@code timeOut} field to the current time in milliseconds.
     * <p>
     * This method updates the {@code timeOut} field of the provided entity to indicate that it is no longer active.
     * The entity is not physically removed from the database.
     * </p>
     *
     * @param entity the entity to softly delete
     * @throws IllegalArgumentException if the entity is {@code null} or does not have an ID
     */
    @Override
    @Transactional
    public void delete(T entity) {

        // Fetch the ID of the entity
        ID entityId = (ID) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);

        if (entityId == null) {
            throw new IllegalArgumentException("Entity must have a valid ID");
        }

        // Fetch the existing entity from the database
        T existingEntity = entityManager.find(domainClass, entityId);
        if (existingEntity == null) {
            throw new IllegalArgumentException("Entity does not exist in the database");
        }

        // Set the `timeOut` field to the current time in milliseconds
        long currentTimeMillis = System.currentTimeMillis();
        existingEntity.setTimeOut(currentTimeMillis);

        // Update the entity in the database
        entityManager.merge(existingEntity);
    }


    /**
     * Performs a soft delete on all entities with the given IDs by setting their {@code timeOut} fields
     * to the current time in milliseconds.
     *
     * @param ids the IDs of the entities to softly delete
     * @throws IllegalArgumentException if any ID is {@code null}
     */
    @Override
    @Transactional
    public void deleteAllById(Iterable<? extends ID> ids) {

        for (ID id : ids) {
            if (id == null) {
                throw new IllegalArgumentException("ID must not be null");
            }

            deleteById(id);
        }
    }


    /**
     * Performs a soft delete on all provided entities by setting their {@code timeOut} fields
     * to the current time in milliseconds.
     *
     * @param entities the entities to softly delete
     * @throws IllegalArgumentException if any entity is {@code null} or does not have a valid ID
     */
    @Override
    @Transactional
    public void deleteAll(Iterable<? extends T> entities) {

        long currentTimeMillis = System.currentTimeMillis();

        for (T entity : entities) {
            if (entity == null) {
                throw new IllegalArgumentException("Entity must not be null");
            }

            delete(entity);
        }
    }


    /**
     * Performs a soft delete on all entities in the database by setting their {@code timeOut} fields
     * to the current time in milliseconds.
     */
    @Override
    @Transactional
    public void deleteAll() {
        long currentTimeMillis = System.currentTimeMillis();

        entityManager.createQuery(
                        "UPDATE " + domainClass.getSimpleName() + " e SET e." + Temporal.TIME_OUT_FIELD + " = :timeOut WHERE e." + Temporal.TIME_OUT_FIELD + " = :infinity")
                .setParameter("timeOut", currentTimeMillis)
                .setParameter("infinity", Temporal.INFINITY)
                .executeUpdate();
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
     * Performs a batch save operation on the provided list of entities.
     * <p>
     * This method updates the {@code timeOut} field of existing entities with the current time
     * in milliseconds and inserts new entities into the database as active entities with their
     * {@code timeOut} field set to {@code Temporal.INFINITY}.
     * The operation is executed in batches to optimize performance for large datasets.
     * </p>
     *
     * @param entities the list of entities to save in batch
     * @throws IllegalArgumentException if the list of entities is {@code null} or empty
     */
    @Transactional
    public void saveInBatch(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        entities.forEach(entityManager::detach);

        // Process entities in batches
        for (int i = 0; i < entities.size(); i += DEFAULT_BATCH_SIZE) {
            // Create a batch slice
            List<T> batch = entities.subList(i, Math.min(i + DEFAULT_BATCH_SIZE, entities.size()));

            // Update the timeOut field for existing entities in the batch
            entityManager.createQuery(
                            "UPDATE " + domainClass.getSimpleName() + " e " +
                                    "SET e." + Temporal.TIME_OUT_FIELD + " = :timeOut " +
                                    "WHERE e." + Temporal.TIME_OUT_FIELD + " = :infinity " +
                                    "AND e.id IN :ids")
                    .setParameter("ids", batch.stream()
                            .map(entity -> (ID) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity))
                            .toList())
                    .setParameter("timeOut", currentTimeMillis)
                    .setParameter("infinity", Temporal.INFINITY)
                    .executeUpdate();

            // Persist new entities and update detached instances
            batch.forEach(entity -> {
                entity.setTimeIn(currentTimeMillis);
                entity.setTimeOut(Temporal.INFINITY);
                entityManager.persist(entity);
            });

            // Flush changes to the database
            entityManager.flush();

            // Detach entities to clear them from the persistence context
            batch.forEach(entityManager::detach);
        }
    }


    /**
     * Performs a batch save operation on the provided list of entities with a specified batch size.
     * <p>
     * This method updates the {@code timeOut} field of existing entities with the current time in milliseconds
     * and inserts new entities as active, with their {@code timeOut} field set to {@code Temporal.INFINITY}.
     * </p>
     * <p>
     * The operation is executed in batches of the specified size to optimize performance for large datasets.
     * </p>
     *
     * @param entities  the list of entities to save in batch
     * @param batchSize the size of each batch for processing
     * @throws IllegalArgumentException if the list of entities is {@code null} or empty, or if batchSize is less than 1
     */
    @Transactional
    public void saveInBatch(List<T> entities, int batchSize) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be greater than 0.");
        }

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        entities.forEach(entityManager::detach);

        // Process entities in batches
        for (int i = 0; i < entities.size(); i += batchSize) {
            // Create a batch slice
            List<T> batch = entities.subList(i, Math.min(i + batchSize, entities.size()));

            // Update the timeOut field for existing entities in the batch
            entityManager.createQuery(
                            "UPDATE " + domainClass.getSimpleName() + " e " +
                                    "SET e." + Temporal.TIME_OUT_FIELD + " = :timeOut " +
                                    "WHERE e." + Temporal.TIME_OUT_FIELD + " = :infinity " +
                                    "AND e.id IN :ids")
                    .setParameter("ids", batch.stream()
                            .map(entity -> (ID) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity))
                            .toList())
                    .setParameter("timeOut", currentTimeMillis)
                    .setParameter("infinity", Temporal.INFINITY)
                    .executeUpdate();

            // Persist new entities and update detached instances
            batch.forEach(entity -> {
                entity.setTimeIn(currentTimeMillis);
                entity.setTimeOut(Temporal.INFINITY);
                entityManager.persist(entity);
            });

            // Flush changes to the database
            entityManager.flush();

            // Detach entities to clear them from the persistence context
            batch.forEach(entityManager::detach);
        }
    }


    /**
     * Performs a batch save operation using the specified {@link EntityManager}.
     * <p>
     * This method updates the {@code timeOut} field of existing entities with the current time in milliseconds
     * and inserts new entities as active, with their {@code timeOut} field set to {@code Temporal.INFINITY}.
     * </p>
     * <p>
     * The operation is executed in batches of the default batch size {@code DEFAULT_BATCH_SIZE} to optimize performance.
     * </p>
     *
     * @param entities the list of entities to save in batch
     * @param em       the {@link EntityManager} to use for the operation
     * @throws IllegalArgumentException if the list of entities is {@code null} or empty, or if the {@code EntityManager} is {@code null}
     */
    @Transactional
    public void saveInBatch(List<T> entities, EntityManager em) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }
        if (em == null) {
            throw new IllegalArgumentException("EntityManager must not be null.");
        }

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        entities.forEach(em::detach);

        // Process entities in batches
        for (int i = 0; i < entities.size(); i += DEFAULT_BATCH_SIZE) {
            // Create a batch slice
            List<T> batch = entities.subList(i, Math.min(i + DEFAULT_BATCH_SIZE, entities.size()));

            // Update the timeOut field for existing entities in the batch
            em.createQuery(
                            "UPDATE " + domainClass.getSimpleName() + " e " +
                                    "SET e." + Temporal.TIME_OUT_FIELD + " = :timeOut " +
                                    "WHERE e." + Temporal.TIME_OUT_FIELD + " = :infinity " +
                                    "AND e.id IN :ids")
                    .setParameter("ids", batch.stream()
                            .map(entity -> (ID) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity))
                            .toList())
                    .setParameter("timeOut", currentTimeMillis)
                    .setParameter("infinity", Temporal.INFINITY)
                    .executeUpdate();

            // Persist new entities and update detached instances
            batch.forEach(entity -> {
                entity.setTimeIn(currentTimeMillis);
                entity.setTimeOut(Temporal.INFINITY);
                em.persist(entity);
            });

            // Flush changes to the database
            em.flush();

            // Detach entities to clear them from the persistence context
            batch.forEach(em::detach);
        }
    }


    /**
     * Performs a soft delete operation on the provided list of entities in batches.
     * <p>
     * This method updates the {@code timeOut} field of existing entities to the current time
     * in milliseconds to mark them as inactive (soft delete).
     * The operation is executed in batches to optimize performance for large datasets.
     * </p>
     *
     * @param entities the list of entities to softly delete in batch
     * @throws IllegalArgumentException if the list of entities is {@code null} or empty
     */
    @Transactional
    public void deleteInBatch(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        entities.forEach(entityManager::detach);

        // Process entities in batches
        for (int i = 0; i < entities.size(); i += DEFAULT_BATCH_SIZE) {
            // Create a batch slice
            List<T> batch = entities.subList(i, Math.min(i + DEFAULT_BATCH_SIZE, entities.size()));

            // Update the timeOut field for existing entities in the batch
            entityManager.createQuery(
                            "UPDATE " + domainClass.getSimpleName() + " e " +
                                    "SET e." + Temporal.TIME_OUT_FIELD + " = :timeOut " +
                                    "WHERE e." + Temporal.TIME_OUT_FIELD + " = :infinity " +
                                    "AND e.id IN :ids")
                    .setParameter("ids", batch.stream()
                            .map(entity -> (ID) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity))
                            .toList())
                    .setParameter("timeOut", currentTimeMillis)
                    .setParameter("infinity", Temporal.INFINITY)
                    .executeUpdate();

            // Detach entities to clear them from the persistence context
            batch.forEach(entityManager::detach);
        }
    }

    /**
     * Performs a soft delete operation on the provided list of entities in batches of a specified size.
     * <p>
     * This method updates the {@code timeOut} field of existing entities to the current time
     * in milliseconds to mark them as inactive (soft delete). The operation is executed in
     * batches of the given size to optimize performance for large datasets.
     * </p>
     *
     * @param entities  the list of entities to softly delete in batch
     * @param batchSize the number of entities to process in each batch
     * @throws IllegalArgumentException if the list of entities is {@code null} or empty
     */
    @Override
    public void deleteInBatch(List<T> entities, int batchSize) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        entities.forEach(entityManager::detach);

        // Process entities in batches
        for (int i = 0; i < entities.size(); i += batchSize) {
            // Create a batch slice
            List<T> batch = entities.subList(i, Math.min(i + batchSize, entities.size()));

            // Update the timeOut field for existing entities in the batch
            entityManager.createQuery(
                            "UPDATE " + domainClass.getSimpleName() + " e " +
                                    "SET e." + Temporal.TIME_OUT_FIELD + " = :timeOut " +
                                    "WHERE e." + Temporal.TIME_OUT_FIELD + " = :infinity " +
                                    "AND e.id IN :ids")
                    .setParameter("ids", batch.stream()
                            .map(entity -> (ID) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity))
                            .toList())
                    .setParameter("timeOut", currentTimeMillis)
                    .setParameter("infinity", Temporal.INFINITY)
                    .executeUpdate();

            // Detach entities to clear them from the persistence context
            batch.forEach(entityManager::detach);
        }
    }


    /**
     * Performs a soft delete operation on the provided list of entities using the given {@link EntityManager}.
     * <p>
     * This method updates the {@code timeOut} field of existing entities to the current time
     * in milliseconds to mark them as inactive (soft delete). The operation is executed in
     * batches of the default size to optimize performance for large datasets.
     * </p>
     *
     * @param entities the list of entities to softly delete in batch
     * @param em       the {@link EntityManager} used to perform the operation
     * @throws IllegalArgumentException if the list of entities is {@code null} or empty
     */
    @Override
    public void deleteInBatch(List<T> entities, EntityManager em) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        entities.forEach(em::detach);

        // Process entities in batches
        for (int i = 0; i < entities.size(); i += DEFAULT_BATCH_SIZE) {
            // Create a batch slice
            List<T> batch = entities.subList(i, Math.min(i + DEFAULT_BATCH_SIZE, entities.size()));

            // Update the timeOut field for existing entities in the batch
            em.createQuery(
                            "UPDATE " + domainClass.getSimpleName() + " e " +
                                    "SET e." + Temporal.TIME_OUT_FIELD + " = :timeOut " +
                                    "WHERE e." + Temporal.TIME_OUT_FIELD + " = :infinity " +
                                    "AND e.id IN :ids")
                    .setParameter("ids", batch.stream()
                            .map(entity -> (ID) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity))
                            .toList())
                    .setParameter("timeOut", currentTimeMillis)
                    .setParameter("infinity", Temporal.INFINITY)
                    .executeUpdate();

            // Detach entities to clear them from the persistence context
            batch.forEach(em::detach);
        }
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
            existingEntity.setTimeOut(currentTimeMillis);
            entityManager.merge(existingEntity);
        }

        // Set the `timeIn` and `timeOut` fields for the new entity
        entity.setTimeIn(currentTimeMillis);
        entity.setTimeOut(Temporal.INFINITY);

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



