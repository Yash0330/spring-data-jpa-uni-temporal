package dev.yash.jpatemporal.parallel;

import dev.yash.jpatemporal.domain.Temporal;
import dev.yash.jpatemporal.repository.TemporalRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ParallelBatchUpdaterTest {

    private ParallelBatchUpdater parallelBatchUpdater;

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityTransaction transaction;

    @Mock
    private TemporalRepository<Temporal, Long> repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getTransaction()).thenReturn(transaction);
        parallelBatchUpdater = new ParallelBatchUpdater(entityManagerFactory);
    }

    @Test
    void testSaveInParallelSuccess() {
        // Arrange
        List<Temporal> entities = createEntities(100);
        int threads = 4;

        // Act
        parallelBatchUpdater.saveInParallel(entities, threads, repository);

        // Assert
        verify(repository, times(4)).saveInBatch(anyList(), eq(entityManager));
        verify(transaction, times(4)).begin();
        verify(transaction, times(4)).commit();
        verify(entityManager, times(4)).close();
    }

    @Test
    void testSaveInParallelThrowsIllegalArgumentExceptionWhenEntitiesNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> parallelBatchUpdater.saveInParallel(null, 4, repository));
    }

    @Test
    void testSaveInParallelThrowsIllegalArgumentExceptionWhenThreadsInvalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> parallelBatchUpdater.saveInParallel(createEntities(100), 0, repository));
    }

    @Test
    void testDeleteInParallelSuccess() {
        // Arrange
        List<Temporal> entities = createEntities(100);
        int threads = 4;

        // Act
        parallelBatchUpdater.deleteInParallel(entities, threads, repository);

        // Assert
        verify(repository, times(4)).deleteInBatch(anyList(), eq(entityManager));
        verify(transaction, times(4)).begin();
        verify(transaction, times(4)).commit();
        verify(entityManager, times(4)).close();
    }

    @Test
    void testDeleteInParallelThrowsIllegalArgumentExceptionWhenEntitiesNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> parallelBatchUpdater.deleteInParallel(null, 4, repository));
    }

    @Test
    void testDeleteInParallelThrowsIllegalArgumentExceptionWhenThreadsInvalid() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> parallelBatchUpdater.deleteInParallel(createEntities(100), 0, repository));
    }

    private List<Temporal> createEntities(int count) {
        List<Temporal> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Temporal entity = mock(Temporal.class);
            entities.add(entity);
        }
        return entities;
    }
}

