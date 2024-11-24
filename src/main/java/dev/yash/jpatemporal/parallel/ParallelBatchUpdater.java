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
     * Saves a list of entities in parallel using multiple threads. The list of entities is divided into sublist,
     * with each sublist being processed by a separate thread. Each thread initializes its own {@link EntityManager}
     * and handles database operations independently. The method ensures efficient batch processing
     * and transaction management in a concurrent environment.
     *
     * <p><strong>Note:</strong> The number of threads should be less than the maximum number of connections
     * allowed by the database to prevent connection exhaustion.</p>
     *
     * @param <T>         the type of the entity, which must implement {@link Temporal}
     * @param <ID>        the type of the entity's identifier
     * @param entities    the list of entities to save; must not be {@code null} or empty
     * @param threads     the number of threads to use for parallel processing; must be greater than 0
     * @param repository  the repository to use for saving entities; must support batch operations
     * @throws IllegalArgumentException if the {@code entities} list is {@code null} or empty,
     *                                  or if {@code threads} is less than or equal to 0
     * @throws RuntimeException         if an error occurs while saving entities or managing transactions
     */
    public <T extends Temporal, ID> void saveInParallel(List<T> entities, int threads, TemporalRepository<T, ID> repository) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0.");
        }

        int batchSize = (entities.size() + threads - 1) / threads; // Calculate the batch size for each thread
        List<List<T>> partitions = new ArrayList<>(threads);

        // Divide entities into sublist
        for (int i = 0; i < entities.size(); i += batchSize) {
            partitions.add(entities.subList(i, Math.min(i + batchSize, entities.size())));
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
                    if(transaction != null){
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
     * Deletes a list of entities in parallel using multiple threads. The list of entities is divided into sublist,
     * with each sublist being processed by a separate thread. Each thread initializes its own {@link EntityManager}
     * and handles database operations independently. The method ensures efficient batch deletion
     * and transaction management in a concurrent environment.
     *
     * <p><strong>Note:</strong> The number of threads should be less than the maximum number of connections
     * allowed by the database to prevent connection exhaustion.</p>
     *
     * @param <T>         the type of the entity, which must implement {@link Temporal}
     * @param <ID>        the type of the entity's identifier
     * @param entities    the list of entities to delete; must not be {@code null} or empty
     * @param threads     the number of threads to use for parallel processing; must be greater than 0
     * @param repository  the repository to use for deleting entities; must support batch operations
     * @throws IllegalArgumentException if the {@code entities} list is {@code null} or empty,
     *                                  or if {@code threads} is less than or equal to 0
     * @throws RuntimeException         if an error occurs while deleting entities or managing transactions
     */
    public <T extends Temporal, ID> void deleteInParallel(List<T> entities, int threads, TemporalRepository<T, ID> repository) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("Entities list must not be null or empty.");
        }
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0.");
        }

        int batchSize = (entities.size() + threads - 1) / threads; // Calculate the batch size for each thread
        List<List<T>> partitions = new ArrayList<>(threads);

        // Divide entities into sublist
        for (int i = 0; i < entities.size(); i += batchSize) {
            partitions.add(entities.subList(i, Math.min(i + batchSize, entities.size())));
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
                    if(transaction != null){
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

