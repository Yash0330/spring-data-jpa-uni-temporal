package dev.yash.jpatemporal.parallel;

import dev.yash.jpatemporal.domain.Temporal;
import dev.yash.jpatemporal.repository.TemporalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static dev.yash.jpatemporal.repository.impl.TemporalRepositoryImpl.filterDuplicatesKeepLast;

/**
 * A utility class for performing batch database operations in parallel using multiple threads.
 * This class supports saving and deleting entities in parallel by dividing the entity list
 * into sublist and processing each sublist independently. Each thread initializes its own
 * {@link EntityManager} and handles database transactions, ensuring efficient batch operations.
 *
 * <p>This class is marked as a Spring {@link Component} to allow dependency injection. It
 * requires an {@link EntityManagerFactory} to create and manage {@link EntityManager} instances
 * for each thread.</p>
 *
 * <p><strong>Usage:</strong> Inject this component into your service or controller and use the
 * provided methods to save or delete entities in parallel.</p>
 *
 * @author Yashwanth, M
 */
@Component
public class ParallelBatchUpdater {

    private final EntityManagerFactory entityManagerFactory;

    public ParallelBatchUpdater(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Saves a list of entities in parallel using multiple threads. This method divides the provided
     * list of entities into sublist, with each sublist being processed by a separate thread. Each thread
     * initializes its own {@link EntityManager} instance and independently manages database operations
     * within a transaction. The method ensures efficient concurrent batch processing while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     *
     * <p><strong>Key Features:</strong></p>
     * <ul>
     *     <li>Filters duplicate entities based on their identifiers and keeps only the last occurrence.</li>
     *     <li>Divides the entities into partitions based on the specified number of threads.</li>
     *     <li>Executes database operations concurrently using multiple threads, with each thread handling
     *         its own transaction.</li>
     *     <li>Ensures proper transaction management, including rollback on failures.</li>
     * </ul>
     *
     * <p><strong>Thread and Resource Management:</strong></p>
     * <ul>
     *     <li>The number of threads should not exceed the maximum connections available in the database
     *         to prevent resource exhaustion.</li>
     *     <li>The method uses a {@link ExecutorService} for managing threads and ensures that the executor
     *         is properly shut down after execution.</li>
     * </ul>
     *
     * @param <T>        The type of the entity, which must implement {@link Temporal}.
     * @param <ID>       The type of the entity's identifier.
     * @param entities   The list of entities to save. Duplicate entries will be filtered,
     *                   retaining only the last occurrence. This parameter must not be {@code null} or empty.
     * @param threads    The number of threads to use for parallel processing. Must be greater than 0.
     * @param repository The repository to use for saving entities. The repository must support batch operations
     *                   and provide a {@code saveInBatch} method for bulk inserts.
     * @throws IllegalArgumentException if:
     *                                  <ul>
     *                                      <li>The {@code entities} list is {@code null} or empty.</li>
     *                                      <li>The {@code threads} value is less than or equal to 0.</li>
     *                                      <li>The {@code repository} is {@code null}.</li>
     *                                  </ul>
     * @throws RuntimeException If an error occurs during the batch save operation, transaction management, or thread execution.
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>
     * {@code
     * List<MyEntity> entities = getEntities();
     * TemporalRepository<MyEntity, Long> repository = getRepository();
     * saveInParallel(entities, 4, repository);
     * }
     * </pre>
     */
    public <T extends Temporal, ID> void saveInParallel(List<T> entities, int threads, TemporalRepository<T, ID> repository) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0.");
        }

        if (repository == null) {
            throw new IllegalArgumentException("Repository must not be null.");
        }

        List<T> noDuplicateEntities = filterDuplicatesKeepLast(entities);

        int batchSize = (noDuplicateEntities.size() + threads - 1) / threads; // Calculate the batch size for each thread
        List<List<T>> partitions = new ArrayList<>(threads);

        // Divide entities into sublist
        for (int i = 0; i < noDuplicateEntities.size(); i += batchSize) {
            partitions.add(noDuplicateEntities.subList(i, Math.min(i + batchSize, noDuplicateEntities.size())));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>(threads);

        for (List<T> sublist : partitions) {
            futures.add(executorService.submit(() -> {
                EntityManager entityManager = entityManagerFactory.createEntityManager();

                EntityTransaction transaction = null;
                try (entityManager) {
                    transaction = entityManager.getTransaction();
                    transaction.begin();
                    repository.saveInBatch(sublist, entityManager); // Save the sublist using the repository
                    transaction.commit();
                } catch (Exception e) {
                    if (transaction != null) {
                        transaction.rollback();
                    }
                    throw new RuntimeException("Error while saving entities", e);
                }
            }));
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("Error executing parallel tasks", e);
            }
        }

        executorService.shutdown();
    }

    /**
     * Deletes a list of entities in parallel using multiple threads. This method divides the provided
     * list of entities into sublist, with each sublist being processed by a separate thread. Each thread
     * initializes its own {@link EntityManager} instance and independently manages database operations
     * within a transaction. The method ensures efficient concurrent batch deletion while avoiding
     * duplicate entities by retaining only the last occurrence of each entity in the input list.
     *
     * <p><strong>Key Features:</strong></p>
     * <ul>
     *     <li>Filters duplicate entities based on their identifiers and keeps only the last occurrence.</li>
     *     <li>Divides the entities into partitions based on the specified number of threads.</li>
     *     <li>Executes database delete operations concurrently using multiple threads, with each thread
     *         handling its own transaction.</li>
     *     <li>Ensures proper transaction management, including rollback on failures.</li>
     * </ul>
     *
     * <p><strong>Thread and Resource Management:</strong></p>
     * <ul>
     *     <li>The number of threads should not exceed the maximum connections available in the database
     *         to prevent resource exhaustion.</li>
     *     <li>The method uses a {@link ExecutorService} for managing threads and ensures that the executor
     *         is properly shut down after execution.</li>
     * </ul>
     *
     * @param <T>        The type of the entity, which must implement {@link Temporal}.
     * @param <ID>       The type of the entity's identifier.
     * @param entities   The list of entities to delete. Duplicate entries will be filtered,
     *                   retaining only the last occurrence. This parameter must not be {@code null} or empty.
     * @param threads    The number of threads to use for parallel processing. Must be greater than 0.
     * @param repository The repository to use for deleting entities. The repository must support batch operations
     *                   and provide a {@code deleteInBatch} method for bulk deletions.
     * @throws IllegalArgumentException if:
     *                                  <ul>
     *                                      <li>The {@code entities} list is {@code null} or empty.</li>
     *                                      <li>The {@code threads} value is less than or equal to 0.</li>
     *                                      <li>The {@code repository} is {@code null}.</li>
     *                                  </ul>
     * @throws RuntimeException If an error occurs during the batch delete operation, transaction management, or thread execution.
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>
     * {@code
     * List<MyEntity> entities = getEntities();
     * TemporalRepository<MyEntity, Long> repository = getRepository();
     * deleteInParallel(entities, 4, repository);
     * }
     * </pre>
     */
    public <T extends Temporal, ID> void deleteInParallel(List<T> entities, int threads, TemporalRepository<T, ID> repository) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0.");
        }

        if (repository == null) {
            throw new IllegalArgumentException("Repository must not be null.");
        }

        List<T> noDuplicateEntities = filterDuplicatesKeepLast(entities);

        int batchSize = (noDuplicateEntities.size() + threads - 1) / threads; // Calculate the batch size for each thread
        List<List<T>> partitions = new ArrayList<>(threads);

        // Divide entities into sublist
        for (int i = 0; i < noDuplicateEntities.size(); i += batchSize) {
            partitions.add(noDuplicateEntities.subList(i, Math.min(i + batchSize, noDuplicateEntities.size())));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>(threads);

        for (List<T> sublist : partitions) {
            futures.add(executorService.submit(() -> {
                EntityManager entityManager = entityManagerFactory.createEntityManager();

                EntityTransaction transaction = null;
                try (entityManager) {
                    transaction = entityManager.getTransaction();
                    transaction.begin();
                    repository.deleteInBatch(sublist, entityManager); // Save the sublist using the repository
                    transaction.commit();
                } catch (Exception e) {
                    if (transaction != null) {
                        transaction.rollback();
                    }
                    throw new RuntimeException("Error while saving entities", e);
                }
            }));
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException("Error executing parallel tasks", e);
            }
        }

        executorService.shutdown();
    }
}

