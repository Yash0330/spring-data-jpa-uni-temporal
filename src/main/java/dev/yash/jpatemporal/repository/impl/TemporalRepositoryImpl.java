package dev.yash.jpatemporal.repository.impl;

import dev.yash.jpatemporal.domain.Temporal;
import dev.yash.jpatemporal.repository.TemporalRepository;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.lang.reflect.Field;
import java.util.*;

import static dev.yash.jpatemporal.domain.Temporal.*;

/**
 * Custom repository implementation for handling temporal entities.
 * <p>
 * This class provides custom implementations of {@link TemporalRepository},
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
@NoRepositoryBean
public class TemporalRepositoryImpl<T extends Temporal, ID> extends SimpleJpaRepository<T, ID> implements TemporalRepository<T, ID> {

    private final Class<T> domainClass;
    private final EntityManager entityManager;

    @Value("${jpa.temporal.batchSize:700}")
    private int DEFAULT_BATCH_SIZE = 700;

    /**
     * Constructs a new {@code TemporalRepositoryImpl}.
     *
     * @param entityInformation Metadata about the entity, including its type and identifier.
     * @param entityManager     The {@code EntityManager} to be used for interacting with the persistence context.
     */
    public TemporalRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.domainClass = entityInformation.getJavaType();
        this.entityManager = entityManager;
    }

    /**
     * Filters duplicates from the input list while preserving the last occurrence of each element.
     * The order of elements in the result list corresponds to the order of their last occurrence
     * in the input list. This method creates a new list and does not modify the input list.
     *
     * <p>Performance characteristics:</p>
     * <ul>
     *   <li>Time complexity: O(n) where n is the size of the input list</li>
     *   <li>Space complexity: O(n) for storing the set</li>
     * </ul>
     *
     * @param <T>      the type of elements in the list
     * @param entities the list to filter duplicates from; must not be null
     * @return a new list containing unique elements in order of their last occurrence
     * @throws NullPointerException if the input list is null
     */
    public static <T> List<T> filterDuplicatesKeepLast(final List<T> entities) {
        Set<T> seen = new HashSet<>();
        List<T> result = new ArrayList<>(entities.size());

        // Single pass in reverse order - O(n)
        for (int i = entities.size() - 1; i >= 0; i--) {
            if (seen.add(entities.get(i))) {
                result.addFirst(entities.get(i));
            }
        }

        return result;
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
                        "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE e." + TIME_OUT_FIELD + " = :timeOut", domainClass)
                .setParameter(TIME_OUT_FIELD, Temporal.INFINITY)
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
    public List<T> findAllById(@Nonnull Iterable<ID> ids) {
        return entityManager.createQuery(
                        "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE e.id IN :ids AND e." + TIME_OUT_FIELD + " = :timeOut", domainClass)
                .setParameter("ids", ids)
                .setParameter(TIME_OUT_FIELD, Temporal.INFINITY)
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
                        "SELECT COUNT(e) FROM " + domainClass.getSimpleName() + " e WHERE e." + TIME_OUT_FIELD + " = :timeOut", Long.class)
                .setParameter(TIME_OUT_FIELD, Temporal.INFINITY)
                .getSingleResult();
    }

    /**
     * Performs a soft delete on entity with the given ID by setting their {@code timeOut} field
     * to the current time in milliseconds.
     *
     * @param id the ID of the entities to softly delete
     */
    @Override
    public void deleteById(@Nonnull ID id) {
        long currentTimeMillis = System.currentTimeMillis();

        T entity = entityManager.find(domainClass, id);
        if (entity != null) {
            entity.setTimeOut(currentTimeMillis);
            entityManager.merge(entity);
            entityManager.flush();
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
    public void delete(@Nonnull T entity) {

        // Fetch the ID of the entity
        ID entityId = getEntityId(entity, entityManager);

        if (entityId == null) {
            throw new IllegalArgumentException("Entity must have a valid ID");
        }

        // Fetch the existing entity from the database
        Optional<T> existingEntityOpt = findByIdExcludingTimeIn(entityId);
        if (existingEntityOpt.isPresent()) {


            T existingEntity = existingEntityOpt.get();

            // Set the `timeOut` field to the current time in milliseconds
            long currentTimeMillis = System.currentTimeMillis();
            existingEntity.setTimeOut(currentTimeMillis);

            // Update the entity in the database
            entityManager.merge(existingEntity);

            entityManager.flush();
        }
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
    public void deleteAllById(@Nonnull Iterable<? extends ID> ids) {

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
    public void deleteAll(@Nonnull Iterable<? extends T> entities) {

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
                        "UPDATE " + domainClass.getSimpleName() + " e SET e." + TIME_OUT_FIELD + " = :timeOut WHERE e." + TIME_OUT_FIELD + " = :infinity")
                .setParameter(TIME_OUT_FIELD, currentTimeMillis)
                .setParameter("infinity", Temporal.INFINITY)
                .executeUpdate();

        entityManager.flush();
    }

    /**
     * Retrieves an entity by its primary key excluding timeIn only if its {@code timeOut} equals {@link Temporal#INFINITY}.
     *
     * @param id the primary key of the entity to retrieve
     * @return an {@code Optional} containing the entity if found and its {@code timeOut} is {@link Temporal#INFINITY}, otherwise empty
     */
    @Override
    @Nonnull
    public Optional<T> findById(@Nonnull ID id) {
        return findByIdExcludingTimeIn(id);
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
    public boolean existsById(@Nonnull ID id) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(e) FROM " + domainClass.getSimpleName() + " e WHERE e.id = :id AND e." + TIME_OUT_FIELD + " = :timeOut", Long.class)
                .setParameter("id", id)
                .setParameter(TIME_OUT_FIELD, Temporal.INFINITY)
                .getSingleResult();
        return count > 0;
    }

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
    @Override
    public boolean existsByIdExcludingTimeIn(@Nonnull ID id) {
        return findByIdExcludingTimeIn(id).isPresent();
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
    public <S extends T> S save(@Nonnull S entity) {
        entityManager.detach(entity);
        // Get the current time in milliseconds
        long currentTimeMillis = System.currentTimeMillis();

        // Get the ID of the entity
        ID entityId = getEntityId(entity, entityManager);

        // Check if the entity exists
        Optional<T> existingEntityOpt = findByIdExcludingTimeIn(entityId);

        if (existingEntityOpt.isPresent()) {
            // Update the `timeOut` field of the existing entity
            T existingEntity = existingEntityOpt.get();
            existingEntity.setTimeOut(currentTimeMillis);
            entityManager.merge(existingEntity);
            entityManager.flush();
        }

        // Set the `timeIn` and `timeOut` fields for the new entity
        entity.setTimeIn(currentTimeMillis);
        entity.setTimeOut(Temporal.INFINITY);

        // Persist the new entity
        entityManager.persist(entity);

        entityManager.flush();

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
    @Transactional
    public <S extends T> List<S> saveAll(@Nonnull Iterable<S> entities) {
        for (T entity : entities) {
            save(entity);
        }
        return (List<S>) entities;
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
                        "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE e." + TIME_OUT_FIELD + " = :timeOut", domainClass)
                .setParameter(TIME_OUT_FIELD, Temporal.INFINITY)
                .setHint("org.hibernate.fetchSize", batchSize)  // Set the fetch size hint to the batch size
                .getResultList();
    }

    /**
     * Finds a single entity by applying a custom predicate and ensuring {@code timeOut} is equal to {@link Temporal#INFINITY}.
     *
     * @param predicate the custom predicate for filtering entities
     * @return an {@code Optional} containing the entity if found, otherwise empty
     */
    @Override
    @Nonnull
    public Optional<T> find(@Nonnull String predicate) {
        T entity = entityManager.createQuery(
                        "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE " + predicate + " AND e." + TIME_OUT_FIELD + " = :timeOut", domainClass)
                .setParameter(TIME_OUT_FIELD, Temporal.INFINITY)
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
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities the list of entities to save in batch
     */
    @Transactional
    @Override
    public void saveInBatch(@Nonnull List<T> entities) {
        if (entities.isEmpty()) {
            return;
        }

        List<T> nonDuplicates = filterDuplicatesKeepLast(entities);

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        nonDuplicates.forEach(entityManager::detach);

        // Process entities in batches
        for (int i = 0; i < nonDuplicates.size(); i += DEFAULT_BATCH_SIZE) {
            // Create a batch slice
            List<T> batch = nonDuplicates.subList(i, Math.min(i + DEFAULT_BATCH_SIZE, nonDuplicates.size()));

            // Convert the batch of entities to their corresponding IDs
            List<ID> entityIds = batch.stream()
                    .map(entity -> getEntityId(entity, entityManager))  // Assuming getId() retrieves the entity's ID
                    .toList();

            // Update the timeOut field for existing entities in the batch
            updateTimeOutExcludingTimeIn(entityIds, currentTimeMillis);

            // Flush changes to the database
            entityManager.flush();

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
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities  the list of entities to save in batch
     * @param batchSize the size of each batch for processing
     * @throws IllegalArgumentException if {@code batchSize} is less than 1
     */
    @Transactional
    @Override
    public void saveInBatch(@Nonnull List<T> entities, int batchSize) {
        if (entities.isEmpty()) {
            return;
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be greater than 0.");
        }

        List<T> nonDuplicates = filterDuplicatesKeepLast(entities);

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        nonDuplicates.forEach(entityManager::detach);

        // Process entities in batches
        for (int i = 0; i < nonDuplicates.size(); i += batchSize) {
            // Create a batch slice
            List<T> batch = nonDuplicates.subList(i, Math.min(i + batchSize, nonDuplicates.size()));

            // Convert the batch of entities to their corresponding IDs
            List<ID> entityIds = batch.stream()
                    .map(entity -> getEntityId(entity, entityManager))  // Assuming getId() retrieves the entity's ID
                    .toList();

            // Update the timeOut field for existing entities in the batch
            updateTimeOutExcludingTimeIn(entityIds, currentTimeMillis);

            // Flush changes to the database
            entityManager.flush();

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
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities the list of entities to save in batch
     * @param em       the {@link EntityManager} to use for the operation
     * @throws IllegalArgumentException if the {@code EntityManager} is {@code null}
     */
    @Transactional
    @Override
    public void saveInBatch(@Nonnull List<T> entities, EntityManager em) {
        if (entities.isEmpty()) {
            return;
        }
        if (em == null) {
            throw new IllegalArgumentException("EntityManager must not be null.");
        }

        List<T> nonDuplicates = filterDuplicatesKeepLast(entities);

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        entities.forEach(em::detach);

        // Process entities in batches
        for (int i = 0; i < entities.size(); i += DEFAULT_BATCH_SIZE) {
            // Create a batch slice
            List<T> batch = entities.subList(i, Math.min(i + DEFAULT_BATCH_SIZE, entities.size()));

            // Convert the batch of entities to their corresponding IDs
            List<ID> entityIds = batch.stream()
                    .map(entity -> getEntityId(entity, entityManager))  // Assuming getId() retrieves the entity's ID
                    .toList();

            // Update the timeOut field for existing entities in the batch
            updateTimeOutExcludingTimeIn(entityIds, currentTimeMillis);

            // Flush changes to the database
            entityManager.flush();

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
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities the list of entities to softly delete in batch
     */
    @Transactional
    @Override
    public void deleteInBatch(@Nonnull List<T> entities) {
        if (entities.isEmpty()) {
            return;
        }

        long currentTimeMillis = System.currentTimeMillis();

        List<T> nonDuplicates = filterDuplicatesKeepLast(entities);

        // Detach all entities to prevent unintended persistence context interference
        nonDuplicates.forEach(entityManager::detach);

        // Process entities in batches
        for (int i = 0; i < nonDuplicates.size(); i += DEFAULT_BATCH_SIZE) {
            // Create a batch slice
            List<T> batch = nonDuplicates.subList(i, Math.min(i + DEFAULT_BATCH_SIZE, nonDuplicates.size()));

            // Convert the batch of entities to their corresponding IDs
            List<ID> entityIds = batch.stream()
                    .map(entity -> getEntityId(entity, entityManager))  // Assuming getId() retrieves the entity's ID
                    .toList();

            // Update the timeOut field for existing entities in the batch
            updateTimeOutExcludingTimeIn(entityIds, currentTimeMillis);

            // Flush changes to the database
            entityManager.flush();

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
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities  the list of entities to softly delete in batch
     * @param batchSize the number of entities to process in each batch
     * @throws IllegalArgumentException if {@code batchSize} is less than 1
     */
    @Override
    @Transactional
    public void deleteInBatch(@Nonnull List<T> entities, int batchSize) {
        if (entities.isEmpty()) {
            return;
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be greater than 0.");
        }

        long currentTimeMillis = System.currentTimeMillis();

        List<T> nonDuplicates = filterDuplicatesKeepLast(entities);

        // Detach all entities to prevent unintended persistence context interference
        nonDuplicates.forEach(entityManager::detach);

        // Process entities in batches
        for (int i = 0; i < nonDuplicates.size(); i += batchSize) {
            // Create a batch slice
            List<T> batch = nonDuplicates.subList(i, Math.min(i + batchSize, nonDuplicates.size()));

            // Convert the batch of entities to their corresponding IDs
            List<ID> entityIds = batch.stream()
                    .map(entity -> getEntityId(entity, entityManager))  // Assuming getId() retrieves the entity's ID
                    .toList();

            // Update the timeOut field for existing entities in the batch
            updateTimeOutExcludingTimeIn(entityIds, currentTimeMillis);

            // Flush changes to the database
            entityManager.flush();

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
     * The method ensures efficient batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     * </p>
     *
     * @param entities the list of entities to softly delete in batch
     * @param em the {@link EntityManager} used to perform the operation
     * @throws IllegalArgumentException if the {@code EntityManager} is {@code null}
     */
    @Override
    @Transactional
    public void deleteInBatch(@Nonnull List<T> entities, EntityManager em) {
        if (entities.isEmpty()) {
            return;
        }
        if (em == null) {
            throw new IllegalArgumentException("EntityManager must not be null.");
        }

        List<T> nonDuplicates = filterDuplicatesKeepLast(entities);

        long currentTimeMillis = System.currentTimeMillis();

        // Detach all entities to prevent unintended persistence context interference
        nonDuplicates.forEach(em::detach);

        // Process entities in batches
        for (int i = 0; i < nonDuplicates.size(); i += DEFAULT_BATCH_SIZE) {
            // Create a batch slice
            List<T> batch = nonDuplicates.subList(i, Math.min(i + DEFAULT_BATCH_SIZE, nonDuplicates.size()));

            // Convert the batch of entities to their corresponding IDs
            List<ID> entityIds = batch.stream()
                    .map(entity -> getEntityId(entity, entityManager))  // Assuming getId() retrieves the entity's ID
                    .toList();

            // Update the timeOut field for existing entities in the batch
            updateTimeOutExcludingTimeIn(entityIds, currentTimeMillis);

            // Flush changes to the database
            entityManager.flush();

            // Detach entities to clear them from the persistence context
            batch.forEach(em::detach);
        }
    }

    /**
     * Finds an entity by its ID, excluding the 'timeIn' field from the composite ID and ensuring
     * the 'timeOut' field matches a specific value (e.g., {@code INFINITY}).
     * <p>
     * This method dynamically builds a query to fetch the entity based on the provided ID fields
     * other than 'timeIn', and adds a condition to filter entities where the 'timeOut' field
     * equals a predefined value.
     * </p>
     *
     * <ul>
     *     <li>The 'timeIn' field is explicitly excluded from the query criteria.</li>
     *     <li>The 'timeOut' field is matched against a predefined value, typically {@code INFINITY}.</li>
     * </ul>
     *
     * @param entityId  The composite ID of the entity, excluding the 'timeIn' field.
     *                  It is assumed that the ID is a composite object containing multiple fields.
     * @return An {@code Optional} containing the found entity if it exists, or an empty {@code Optional} if no entity matches the criteria.
     */
    public Optional<T> findByIdExcludingTimeIn(ID entityId) {
        // Obtain the EntityManager and CriteriaBuilder
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(domainClass);
        Root<T> root = cq.from(domainClass);

        // Get the composite ID path
        Path<ID> idPath = root.get("id");

        // Dynamically create predicates (conditions) excluding timeIn
        Predicate[] predicates = buildPredicatesExcludingTimeIn(cb, idPath, entityId);

        if (predicates.length == 0) {
            return Optional.empty();
        }

        // Create a predicate to check that timeOut equals INFINITY
        Predicate timeOutPredicate = cb.equal(root.get(TIME_OUT_FIELD), INFINITY);

        // Combine all predicates (ID predicates + timeOut predicate)
        Predicate finalPredicate = cb.and(cb.and(predicates), timeOutPredicate);

        // Apply the predicates to the CriteriaQuery
        cq.where(finalPredicate);

        // Create the query and execute it
        TypedQuery<T> query = entityManager.createQuery(cq);
        try {
            T entity = query.getSingleResult();
            return Optional.of(entity);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Builds dynamic predicates for the query, excluding the 'timeIn' field from the ID.
     * This method uses Java reflection to inspect the fields of the composite ID and
     * dynamically constructs conditions for the query based on non-null fields.
     *
     * @param cb       The CriteriaBuilder used to create the query predicates.
     * @param idPath   The path representing the composite ID in the entity.
     * @param entityId The ID object containing the fields for the entity to be queried.
     * @return An array of predicates to be used in the query.
     */
    private Predicate[] buildPredicatesExcludingTimeIn(CriteriaBuilder cb, Path<ID> idPath, ID entityId) {
        // Use reflection to dynamically inspect fields of the ID class
        Field[] fields = entityId.getClass().getDeclaredFields();

        List<Predicate> predicates = new ArrayList<>(fields.length - 1);

        // Iterate over the fields and create predicates for each field (excluding timeIn)
        for (Field field : fields) {
            // Skip the 'timeIn' field
            if (TIME_IN_FIELD.equals(field.getName())) {
                continue;
            }

            // Make sure the field is accessible
            field.setAccessible(true);

            try {
                // Get the value of the field from the entityId instance
                Object fieldValue = field.get(entityId);

                // Add a predicate for non-null fields
                if (fieldValue != null) {
                    predicates.add(cb.equal(idPath.get(field.getName()), fieldValue));
                }
            } catch (IllegalAccessException e) {
                // Handle reflection access exception if needed
                throw new RuntimeException(e);
            }
        }

        // Return the array of predicates
        return predicates.toArray(new Predicate[0]);
    }

    /**
     * Updates the 'timeOut' field for entities that match any of the specified IDs, excluding the 'timeIn' field.
     * <p>
     * This method dynamically constructs an update query to modify the 'timeOut' value for all entities
     * whose composite ID fields (excluding 'timeIn') match any of the provided entity IDs.
     * Additionally, the update ensures that the 'timeOut' field is modified based on the provided value.
     * </p>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *     <li>The 'timeIn' field is excluded from the matching criteria during the update.</li>
     *     <li>Entities are updated where the 'timeOut' field currently has a predefined value, typically {@code INFINITY}.</li>
     *     <li>The 'timeOut' field is set to the new value passed as a parameter.</li>
     *     <li>The method applies the update for all entity IDs provided in the list.</li>
     * </ul>
     *
     * <p><b>Usage:</b></p>
     * <pre>
     * int updatedCount = entityService.updateTimeOutExcludingTimeIn(entityIds, newTimeOut);
     * System.out.println("Number of entities updated: " + updatedCount);
     * </pre>
     *
     * @param entityIds A list of composite IDs for the entities (excluding the 'timeIn' field).
     *                  It is assumed that the IDs are composite objects containing multiple fields.
     * @param timeOut   The new value for the 'timeOut' field to be set in the matching entities.
     */
    private void updateTimeOutExcludingTimeIn(List<ID> entityIds, Long timeOut) {
        // Obtain the CriteriaBuilder and CriteriaUpdate
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<T> criteriaUpdate = cb.createCriteriaUpdate(domainClass);

        // Define the root for the entity
        Root<T> root = criteriaUpdate.from(domainClass);

        // Create an empty list for predicates
        List<Predicate> allPredicates = new ArrayList<>();

        // Dynamically build the predicates for the ID excluding 'timeIn' for each entityId
        for (ID entityId : entityIds) {
            Predicate[] predicates = buildPredicatesExcludingTimeIn(cb, root.get("id"), entityId);

            if (predicates.length > 0) {
                // Add the individual entity ID predicates to the list of all predicates
                allPredicates.add(cb.and(predicates));
            }
        }

        if (allPredicates.isEmpty()) {
            return; // No matching IDs
        }

        // Combine all predicates using OR for each ID
        Predicate combinedPredicate = cb.or(allPredicates.toArray(new Predicate[0]));

        // Create a predicate to check that timeOut equals INFINITY
        Predicate timeOutPredicate = cb.equal(root.get(TIME_OUT_FIELD), INFINITY);

        // Combine the ID predicates with the timeOut condition
        Predicate finalPredicate = cb.and(combinedPredicate, timeOutPredicate);

        // Set the 'timeOut' value to the new value
        criteriaUpdate.set(TIME_OUT_FIELD, timeOut);

        // Apply the predicates as the condition for the update query
        criteriaUpdate.where(finalPredicate);

        // Create the query and execute it
        Query query = entityManager.createQuery(criteriaUpdate);
        query.executeUpdate();
    }

    /**
     * Retrieves the ID of an entity using the EntityManager.
     *
     * @param entity        The entity object.
     * @param entityManager The EntityManager used to retrieve the ID.
     * @return The ID of the entity.
     */
    private ID getEntityId(T entity, EntityManager entityManager) {
        // Use EntityManager to retrieve the identifier of the entity
        return (ID) entityManager.getEntityManagerFactory()
                .getPersistenceUnitUtil()
                .getIdentifier(entity);
    }
}



