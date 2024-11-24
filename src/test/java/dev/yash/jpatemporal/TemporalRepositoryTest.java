package dev.yash.jpatemporal;

import dev.yash.jpatemporal.annotation.EnableJpaTemporalRepositories;
import dev.yash.jpatemporal.domain.Temporal;
import dev.yash.jpatemporal.domain.Transaction;
import dev.yash.jpatemporal.domain.TransactionId;
import dev.yash.jpatemporal.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(classes = TemporalRepositoryTest.TestConfig.class)
@SpringJUnitConfig
class TemporalRepositoryTest {

    long timeIn = System.currentTimeMillis();
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TransactionRepository repository;
    private Transaction activeTransaction1;
    private Transaction activeTransaction2;
    private Transaction activeTransaction3;
    private Transaction inactiveTransaction1;
    private Transaction inactiveTransaction2;

    @BeforeEach
    void setUp() {
        // Create multiple active transactions
        activeTransaction1 = createTransaction("TX001", "ACC001", "DEPOSIT", 1000.0, Temporal.INFINITY, timeIn);
        activeTransaction2 = createTransaction("TX002", "ACC001", "WITHDRAWAL", 500.0, Temporal.INFINITY, timeIn);
        activeTransaction3 = createTransaction("TX003", "ACC002", "DEPOSIT", 1500.0, Temporal.INFINITY, timeIn);

        // Create inactive transactions
        inactiveTransaction1 = createTransaction("TX004", "ACC002", "WITHDRAWAL", 300.0, System.currentTimeMillis(), timeIn);
        inactiveTransaction2 = createTransaction("TX005", "ACC003", "DEPOSIT", 2000.0, System.currentTimeMillis(), timeIn);

        // Persist all transactions
        Arrays.asList(activeTransaction1, activeTransaction2, activeTransaction3,
                        inactiveTransaction1, inactiveTransaction2)
                .forEach(entityManager::persist);

        entityManager.flush();
    }

    private Transaction createTransaction(String id, String accountId, String type, double amount, long timeOut, long timeIn) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(id);
        transaction.setAccountId(accountId);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setTimeOut(timeOut);
        transaction.setTimeIn(timeIn);
        transaction.setTimeIn(timeIn);
        return transaction;
    }

    @Test
    void findAll_ShouldReturnOnlyActiveTransactions() {
        // When
        List<Transaction> result = repository.findAll();

        // Then
        assertThat(result)
                .hasSize(3)
                .contains(activeTransaction1, activeTransaction2, activeTransaction3)
                .doesNotContain(inactiveTransaction1, inactiveTransaction2)
                .allMatch(t -> Objects.equals(t.getTimeOut(), Temporal.INFINITY));
    }

    @Test
    void findAll_WhenNoActiveTransactions_ShouldReturnEmptyList() {
        // Given
        repository.deleteAll();

        // When
        List<Transaction> result = repository.findAll();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findAllById_ShouldReturnOnlyRequestedActiveTransactions() {
        // Given
        List<TransactionId> ids = Arrays.asList(new TransactionId(timeIn, "TX001"), new TransactionId(timeIn, "TX002"), new TransactionId(timeIn, "TX004"));


        // When
        List<Transaction> result = repository.findAllById(ids);

        // Then
        assertThat(result)
                .hasSize(2)
                .contains(activeTransaction1, activeTransaction2)
                .allMatch(t -> Objects.equals(t.getTimeOut(), Temporal.INFINITY));
    }

    @Test
    void findAllById_WithNonExistentIds_ShouldReturnOnlyExistingActiveTransactions() {
        // Given
        List<TransactionId> ids = Arrays.asList(new TransactionId(timeIn, "TX001"), new TransactionId(timeIn, "NONEXISTENT1"), new TransactionId(timeIn, "NONEXISTENT2"));

        // When
        List<Transaction> result = repository.findAllById(ids);

        // Then
        assertThat(result)
                .hasSize(1)
                .contains(activeTransaction1)
                .allMatch(t -> Objects.equals(t.getTimeOut(), Temporal.INFINITY));
    }

    @Test
    void findAllById_WithEmptyIdList_ShouldReturnEmptyList() {
        // Given
        List<TransactionId> emptyIds = new ArrayList<>();

        // When
        List<Transaction> result = repository.findAllById(emptyIds);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findAllById_WhenAllTransactionsInactive_ShouldReturnEmptyList() {
        // Given
        repository.deleteAll();

        List<TransactionId> ids = Arrays.asList(new TransactionId(timeIn, "TX001"), new TransactionId(timeIn, "TX002"));

        // When
        List<Transaction> result = repository.findAllById(ids);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void count_ShouldReturnNumberOfActiveTransactions() {
        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void count_AfterDeactivatingAllTransactions_ShouldReturnZero() {
        // Given
        repository.deleteAll();

        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void count_AfterAddingMoreActiveTransactions_ShouldReturnUpdatedCount() {
        // Given
        Transaction newActiveTransaction = createTransaction("TX006", "ACC004", "DEPOSIT", 3000.0, Temporal.INFINITY, timeIn);
        entityManager.persist(newActiveTransaction);
        entityManager.flush();

        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(4);
    }

    @Test
    void deleteById_ShouldSoftDeleteExistingTransaction() {
        // Given
        String transactionId = activeTransaction1.getTransactionId();

        // When
        repository.deleteById(new TransactionId(timeIn, transactionId));
        entityManager.flush();

        // Then
        Transaction deletedTransaction = entityManager.find(Transaction.class, new TransactionId(timeIn, transactionId));
        assertNotNull(deletedTransaction);
        assertNotEquals(Temporal.INFINITY, deletedTransaction.getTimeOut());
        assertTrue(deletedTransaction.getTimeOut() <= System.currentTimeMillis());
    }

    @Test
    void deleteById_WithNonExistentId_ShouldNotThrowException() {
        // Given
        String nonExistentId = "NONEXISTENT";

        // When/Then
        assertDoesNotThrow(() -> repository.deleteById(new TransactionId(timeIn, nonExistentId)));
    }

    @Test
    void delete_ShouldSoftDeleteExistingTransaction() {
        // Given
        Transaction transaction = activeTransaction1;

        // When
        repository.delete(transaction);
        entityManager.flush();

        // Then
        Transaction deletedTransaction = entityManager.find(Transaction.class, new TransactionId(timeIn, transaction.getTransactionId()));
        assertNotNull(deletedTransaction);
        assertNotEquals(Temporal.INFINITY, deletedTransaction.getTimeOut());
        assertTrue(deletedTransaction.getTimeOut() <= System.currentTimeMillis());
    }

    @Test
    void delete_WithNullEntity_ShouldThrowException() {
        // When/Then
        assertThrows(InvalidDataAccessApiUsageException.class, () -> repository.delete(null));
    }

    @Test
    void delete_WithNullEntityId_ShouldThrowException() {
        // When/Then
        assertThrows(InvalidDataAccessApiUsageException.class, () -> repository.delete(new Transaction()));
    }

    @Test
    void delete_WithNonExistentEntity_ShouldThrowException() {
        // Given
        Transaction nonExistentTransaction = createTransaction("NONEXISTENT", "ACC999", "DEPOSIT", 1000.0, Temporal.INFINITY, timeIn);

        // When/Then
        assertThrows(InvalidDataAccessApiUsageException.class, () -> repository.delete(nonExistentTransaction));
    }

    @Test
    void deleteAllById_ShouldSoftDeleteMultipleTransactions() {
        // Given
        List<TransactionId> ids = Arrays.asList(
                new TransactionId(timeIn, activeTransaction1.getTransactionId()),
                new TransactionId(timeIn, activeTransaction2.getTransactionId())
        );


        // When
        repository.deleteAllById(ids);
        entityManager.flush();

        // Then
        for (TransactionId id : ids) {
            Transaction deletedTransaction = entityManager.find(Transaction.class, id);
            assertNotNull(deletedTransaction);
            assertNotEquals(Temporal.INFINITY, deletedTransaction.getTimeOut());
            assertTrue(deletedTransaction.getTimeOut() <= System.currentTimeMillis());
        }
    }

    @Test
    void deleteAllById_WithNullId_ShouldThrowException() {
        // Given

        List<TransactionId> ids = Arrays.asList(
                new TransactionId(timeIn, activeTransaction1.getTransactionId()),
                null,
                new TransactionId(timeIn, activeTransaction2.getTransactionId())
        );

        // When/Then
        assertThrows(InvalidDataAccessApiUsageException.class, () -> repository.deleteAllById(ids));
    }

    @Test
    void deleteAll_ShouldSoftDeleteMultipleTransactions() {
        // Given
        List<Transaction> transactions = Arrays.asList(
                activeTransaction1,
                activeTransaction2
        );

        // When
        repository.deleteAll(transactions);
        entityManager.flush();

        // Then
        for (Transaction transaction : transactions) {
            Transaction deletedTransaction = entityManager.find(Transaction.class, new TransactionId(timeIn, transaction.getTransactionId()));
            assertNotNull(deletedTransaction);
            assertNotEquals(Temporal.INFINITY, deletedTransaction.getTimeOut());
            assertTrue(deletedTransaction.getTimeOut() <= System.currentTimeMillis());
        }
    }

    @Test
    void deleteAll_WithNullEntity_ShouldThrowException() {
        // Given
        List<Transaction> transactions = Arrays.asList(
                activeTransaction1,
                null,
                activeTransaction2
        );

        // When/Then
        assertThrows(InvalidDataAccessApiUsageException.class, () -> repository.deleteAll(transactions));
    }

    @Test
    void deleteAll_WithNonExistentEntity_ShouldThrowException() {
        // Given
        Transaction nonExistentTransaction = createTransaction("NONEXISTENT", "ACC999", "DEPOSIT", 1000.0, Temporal.INFINITY, timeIn);
        List<Transaction> transactions = Arrays.asList(
                activeTransaction1,
                nonExistentTransaction,
                activeTransaction2
        );

        // When/Then
        assertThrows(InvalidDataAccessApiUsageException.class, () -> repository.deleteAll(transactions));
    }

    @Test
    void deleteAll_ShouldSoftDeleteAllActiveTransactions() {
        // Given
        long initialCount = entityManager.createQuery(
                        "SELECT COUNT(t) FROM Transaction t WHERE t.timeOut = :infinity", Long.class)
                .setParameter("infinity", Temporal.INFINITY)
                .getSingleResult();
        assertTrue(initialCount > 0, "Should have active transactions before test");

        // When
        repository.deleteAll();

        // Then
        long activeCount = entityManager.createQuery(
                        "SELECT COUNT(t) FROM Transaction t WHERE t.timeOut = :infinity", Long.class)
                .setParameter("infinity", Temporal.INFINITY)
                .getSingleResult();
        assertEquals(0, activeCount, "No transactions should be active after deleteAll");

        // Verify previously inactive transactions remain unchanged
        Transaction stillInactive1 = entityManager.find(Transaction.class, new TransactionId(timeIn, inactiveTransaction1.getTransactionId()));
        Transaction stillInactive2 = entityManager.find(Transaction.class, new TransactionId(timeIn, inactiveTransaction2.getTransactionId()));
        assertEquals(inactiveTransaction1.getTimeOut(), stillInactive1.getTimeOut());
        assertEquals(inactiveTransaction2.getTimeOut(), stillInactive2.getTimeOut());
    }

    @Test
    void findById_WithActiveTransaction_ShouldReturnTransaction() {
        // Given
        String activeId = activeTransaction1.getTransactionId();

        // When
        Optional<Transaction> result = repository.findById(new TransactionId(timeIn, activeId));

        // Then
        assertTrue(result.isPresent());
        assertEquals(activeId, result.get().getTransactionId());
        assertEquals(Temporal.INFINITY, result.get().getTimeOut());
    }

    @Test
    void findById_WithInactiveTransaction_ShouldReturnEmpty() {
        // Given
        String inactiveId = inactiveTransaction1.getTransactionId();

        // When
        Optional<Transaction> result = repository.findById(new TransactionId(timeIn, inactiveId));

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void findById_WithNonExistentId_ShouldReturnEmpty() {
        // Given
        String nonExistentId = "NONEXISTENT";

        // When
        Optional<Transaction> result = repository.findById(new TransactionId(timeIn, nonExistentId));

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void existsById_WithActiveTransaction_ShouldReturnTrue() {
        // Given
        String activeId = activeTransaction1.getTransactionId();

        // When
        boolean exists = repository.existsById(new TransactionId(timeIn, activeId));

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_WithInactiveTransaction_ShouldReturnFalse() {
        // Given
        String inactiveId = inactiveTransaction1.getTransactionId();

        // When
        boolean exists = repository.existsById(new TransactionId(timeIn, inactiveId));

        // Then
        assertFalse(exists);
    }

    @Test
    void existsById_WithNonExistentId_ShouldReturnFalse() {
        // Given
        String nonExistentId = "NONEXISTENT";

        // When
        boolean exists = repository.existsById(new TransactionId(timeIn, nonExistentId));

        // Then
        assertFalse(exists);
    }

    @Test
    void deleteAll_ShouldNotAffectAlreadyInactiveTransactions() {
        // Given
        long originalTimeOut = inactiveTransaction1.getTimeOut();

        // When
        repository.deleteAll();
        entityManager.flush();

        // Then
        Transaction transaction = entityManager.find(Transaction.class, new TransactionId(timeIn, inactiveTransaction1.getTransactionId()));
        assertEquals(originalTimeOut, transaction.getTimeOut());
    }

    @Test
    void findById_AfterDeletion_ShouldReturnEmpty() {
        // Given
        String activeId = activeTransaction1.getTransactionId();
        assertTrue(repository.findById(new TransactionId(timeIn, activeId)).isPresent());

        // When
        repository.deleteById(new TransactionId(timeIn, activeId));
        entityManager.flush();

        // Then
        Optional<Transaction> result = repository.findById(new TransactionId(timeIn, activeId));
        assertFalse(result.isPresent());
    }

    @Test
    void existsById_AfterDeletion_ShouldReturnFalse() {
        // Given
        String activeId = activeTransaction1.getTransactionId();
        assertTrue(repository.existsById(new TransactionId(timeIn, activeId)));

        // When
        repository.deleteById(new TransactionId(timeIn, activeId));
        entityManager.flush();

        // Then
        assertFalse(repository.existsById(new TransactionId(timeIn, activeId)));
    }

    @Test
    void save_NewEntity_ShouldPersistWithTimeOutSetToInfinity() {
        // Arrange
        Transaction newTransaction = new Transaction();
        newTransaction.setTransactionId("TX006");
        newTransaction.setAccountId("ACC004");
        newTransaction.setTransactionType("DEPOSIT");
        newTransaction.setAmount(2500.0);

        // Act
        Transaction savedTransaction = repository.save(newTransaction);

        // Assert
        assertThat(savedTransaction.getTransactionId()).isEqualTo("TX006");
        assertThat(savedTransaction.getTimeOut()).isEqualTo(Temporal.INFINITY);
        assertThat(savedTransaction.getTimeIn()).isNotNull();
    }

    @Test
    void save_ExistingEntity_ShouldUpdateExistingAndSaveNewWithTimeOutSetToInfinity() {
        // Arrange
        String transactionId = "TX001";
        Optional<Transaction> existingTransactionOpt = repository.findById(new TransactionId(timeIn, transactionId));
        assertThat(existingTransactionOpt).isPresent();

        Transaction existingTransaction = existingTransactionOpt.get();
        long previousTimeOut = existingTransaction.getTimeOut();

        // Update the existing transaction details
        existingTransaction.setAmount(2000.0);

        // Act
        Transaction newSavedTransaction = repository.save(existingTransaction);

        // Verify the new transaction is saved
        assertThat(newSavedTransaction.getTransactionId()).isEqualTo(transactionId);
        assertThat(newSavedTransaction.getTimeOut()).isEqualTo(Temporal.INFINITY);
        assertThat(newSavedTransaction.getAmount()).isEqualTo(2000.0);
        assertThat(newSavedTransaction.getTimeIn()).isNotNull();
    }

    @Test
    void save_ExistingEntity_ShouldUpdateExistingAndSaveNewWithTimeOutSetToInfinityCheck() {
        // Arrange
        String transactionId = "TX001";
        Optional<Transaction> existingTransactionOpt = repository.findById(new TransactionId(timeIn, transactionId));
        assertThat(existingTransactionOpt).isPresent();

        Transaction existingTransaction = existingTransactionOpt.get();

        // Update the existing transaction details
        existingTransaction.setAmount(2000.0);

        // Act
        Transaction newSavedTransaction = repository.save(existingTransaction);

        // Verify the existing transaction is updated
        existingTransaction = entityManager.find(Transaction.class, new TransactionId(timeIn, transactionId));
        assertThat(existingTransaction.getTimeOut()).isLessThan(System.currentTimeMillis());
        assertThat(existingTransaction.getAmount()).isEqualTo(1000.0);

        // Verify the new transaction is saved
        assertThat(newSavedTransaction.getTransactionId()).isEqualTo(transactionId);
        assertThat(newSavedTransaction.getTimeOut()).isEqualTo(Temporal.INFINITY);
        assertThat(newSavedTransaction.getAmount()).isEqualTo(2000.0);
        assertThat(newSavedTransaction.getTimeIn()).isNotNull();
    }

    @Test
    void saveAll_NewEntities_ShouldPersistWithTimeOutSetToInfinity() {
        // Arrange
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionId("TX006");
        transaction1.setAccountId("ACC004");
        transaction1.setTransactionType("DEPOSIT");
        transaction1.setAmount(2500.0);

        Transaction transaction2 = new Transaction();
        transaction2.setTransactionId("TX007");
        transaction2.setAccountId("ACC005");
        transaction2.setTransactionType("WITHDRAWAL");
        transaction2.setAmount(1000.0);

        List<Transaction> newTransactions = List.of(transaction1, transaction2);

        // Act
        List<Transaction> savedTransactions = (List<Transaction>) repository.saveAll(newTransactions);

        // Assert
        assertThat(savedTransactions).hasSize(2);
        for (Transaction saved : savedTransactions) {
            assertThat(saved.getTimeOut()).isEqualTo(Temporal.INFINITY);
            assertThat(saved.getTimeIn()).isNotNull();
        }
    }

    @Test
    void saveAll_ExistingEntities_ShouldUpdateExistingAndSaveNewWithTimeOutSetToInfinity() {
        // Arrange
        Optional<Transaction> existingTransactionOpt1 = repository.findById(new TransactionId(timeIn, "TX001"));
        Optional<Transaction> existingTransactionOpt2 = repository.findById(new TransactionId(timeIn, "TX002"));

        assertThat(existingTransactionOpt1).isPresent();
        assertThat(existingTransactionOpt2).isPresent();

        Transaction transaction1 = existingTransactionOpt1.get();
        transaction1.setAmount(3000.0); // Modify amount

        Transaction transaction2 = existingTransactionOpt2.get();
        transaction2.setAmount(2000.0); // Modify amount

        List<Transaction> transactionsToUpdate = List.of(transaction1, transaction2);

        // Act
        List<Transaction> updatedTransactions = (List<Transaction>) repository.saveAll(transactionsToUpdate);

        // Assert
        assertThat(updatedTransactions).hasSize(2);

        for (Transaction updated : updatedTransactions) {
            assertThat(updated.getTimeOut()).isEqualTo(Temporal.INFINITY);
            assertThat(updated.getTimeIn()).isNotNull();
        }

        Optional<Transaction> updatedTransaction1Opt = repository.findAll().stream().filter(t -> t.getTransactionId().equals("TX001")).findFirst();
        assertThat(updatedTransaction1Opt).isPresent();
        assertThat(updatedTransaction1Opt.get().getAmount()).isEqualTo(3000.0);
    }

    @Test
    void findAllByBatchSize_ShouldReturnEntitiesWithTimeOutSetToInfinity() {
        // Arrange
        int batchSize = 2;

        // Act
        List<Transaction> activeTransactions = repository.findAllByBatchSize(batchSize);

        // Assert
        assertThat(activeTransactions).isNotEmpty();
        assertThat(activeTransactions.size()).isEqualTo(3);
        for (Transaction transaction : activeTransactions) {
            assertThat(transaction.getTimeOut()).isEqualTo(Temporal.INFINITY);
        }
    }

    @Test
    void findAllByBatchSize_NoEntitiesWithTimeOutInfinity_ShouldReturnEmptyList() {
        // Arrange
        repository.deleteAll();

        int batchSize = 2;

        // Act
        List<Transaction> activeTransactions = repository.findAllByBatchSize(batchSize);

        // Assert
        assertThat(activeTransactions).isEmpty();
    }

    @Test
    void find_WithValidPredicate_ShouldReturnMatchingEntity() {
        // Arrange
        String predicate = "e.transactionId = 'TX001'";

        // Act
        Optional<Transaction> foundTransaction = repository.find(predicate);

        // Assert
        assertThat(foundTransaction).isPresent();
        assertThat(foundTransaction.get().getTransactionId()).isEqualTo("TX001");
        assertThat(foundTransaction.get().getTimeOut()).isEqualTo(Temporal.INFINITY);
    }

    @Test
    void find_WithInvalidPredicate_ShouldReturnEmptyOptional() {
        // Arrange
        String predicate = "e.transactionId = 'INVALID_TX'";

        // Act
        Optional<Transaction> foundTransaction = repository.find(predicate);

        // Assert
        assertThat(foundTransaction).isEmpty();
    }

    @Test
    void find_WithPredicateForInactiveEntity_ShouldReturnEmptyOptional() {
        // Arrange
        // Make TX001 inactive (not Temporal.INFINITY)
        Optional<Transaction> inactiveTransactionOpt = repository.findById(new TransactionId(timeIn, "TX001"));
        assertThat(inactiveTransactionOpt).isPresent();

        Transaction inactiveTransaction = inactiveTransactionOpt.get();
        repository.delete(inactiveTransaction);

        String predicate = "e.transactionId = 'TX001'";

        // Act
        Optional<Transaction> foundTransaction = repository.find(predicate);

        // Assert
        assertThat(foundTransaction).isEmpty();
    }

    @Test
    void saveInBatch_NewEntities_ShouldPersistWithTimeOutSetToInfinity() {
        // Arrange
        Transaction transaction1 = new Transaction();
        transaction1.setTransactionId("TX006");
        transaction1.setAccountId("ACC004");
        transaction1.setTransactionType("DEPOSIT");
        transaction1.setAmount(2500.0);

        Transaction transaction2 = new Transaction();
        transaction2.setTransactionId("TX007");
        transaction2.setAccountId("ACC005");
        transaction2.setTransactionType("WITHDRAWAL");
        transaction2.setAmount(1000.0);


        List<Transaction> newTransactions = List.of(transaction1, transaction2);

        // Act
        repository.saveInBatch(newTransactions);

        List<Transaction> savedTransactions = repository.findAll().stream().filter(t -> t.getTransactionId().equals("TX006") || t.getTransactionId().equals("TX007")).toList();

        // Assert
        assertThat(savedTransactions).hasSize(2);
        for (Transaction saved : savedTransactions) {
            assertThat(saved.getTimeOut()).isEqualTo(Temporal.INFINITY);
            assertThat(saved.getTimeIn()).isNotNull();
        }
    }

    @Test
    void saveInBatch_ExistingEntities_ShouldUpdateExistingAndSaveNewWithTimeOutSetToInfinity() {
        // Arrange
        Optional<Transaction> existingTransactionOpt1 = repository.findById(new TransactionId(timeIn, "TX001"));
        Optional<Transaction> existingTransactionOpt2 = repository.findById(new TransactionId(timeIn, "TX002"));


        assertThat(existingTransactionOpt1).isPresent();
        assertThat(existingTransactionOpt2).isPresent();

        Transaction transaction1 = existingTransactionOpt1.get();
        transaction1.setAmount(3000.0); // Modify amount

        Transaction transaction2 = existingTransactionOpt2.get();
        transaction2.setAmount(2000.0); // Modify amount

        List<Transaction> transactionsToUpdate = List.of(transaction1, transaction2);

        // Act
        repository.saveInBatch(transactionsToUpdate);

        List<Transaction> updatedTransactions = repository.findAll().stream().filter(t -> t.getTransactionId().equals("TX001") || t.getTransactionId().equals("TX002")).toList();

        // Assert
        assertThat(updatedTransactions).hasSize(2);

        for (Transaction updated : updatedTransactions) {
            assertThat(updated.getTimeOut()).isEqualTo(Temporal.INFINITY);
            assertThat(updated.getTimeIn()).isNotNull();
        }

        Optional<Transaction> updatedTransaction1Opt = updatedTransactions.stream().filter(t -> t.getTransactionId().equals("TX001")).findFirst();
        assertThat(updatedTransaction1Opt).isPresent();
        assertThat(updatedTransaction1Opt.get().getAmount()).isEqualTo(3000.0);
    }

    @Test
    void saveInBatchWithBatchSize_ExistingEntities_ShouldUpdateExistingAndSaveNewWithTimeOutSetToInfinity() {
        // Arrange
        Optional<Transaction> existingTransactionOpt1 = repository.findById(new TransactionId(timeIn, "TX001"));
        Optional<Transaction> existingTransactionOpt2 = repository.findById(new TransactionId(timeIn, "TX002"));

        assertThat(existingTransactionOpt1).isPresent();
        assertThat(existingTransactionOpt2).isPresent();

        Transaction transaction1 = existingTransactionOpt1.get();
        transaction1.setAmount(3000.0); // Modify amount

        Transaction transaction2 = existingTransactionOpt2.get();
        transaction2.setAmount(2000.0); // Modify amount

        List<Transaction> transactionsToUpdate = List.of(transaction1, transaction2);

        // Act
        repository.saveInBatch(transactionsToUpdate, 1);

        List<Transaction> updatedTransactions = repository.findAll().stream().filter(t -> t.getTransactionId().equals("TX001") || t.getTransactionId().equals("TX002")).toList();

        // Assert
        assertThat(updatedTransactions).hasSize(2);

        for (Transaction updated : updatedTransactions) {
            assertThat(updated.getTimeOut()).isEqualTo(Temporal.INFINITY);
            assertThat(updated.getTimeIn()).isNotNull();
        }

        Optional<Transaction> updatedTransaction1Opt = updatedTransactions.stream().filter(t -> t.getTransactionId().equals("TX001")).findFirst();
        assertThat(updatedTransaction1Opt).isPresent();
        assertThat(updatedTransaction1Opt.get().getAmount()).isEqualTo(3000.0);
    }

    @Test
    void saveInBatch_ExistingEntities_ShouldUpdateExistingAndSaveNewWithTimeOutSetToInfinityWithEm() {

        // Arrange
        Optional<Transaction> existingTransactionOpt1 = repository.findById(new TransactionId(timeIn, "TX001"));
        Optional<Transaction> existingTransactionOpt2 = repository.findById(new TransactionId(timeIn, "TX002"));

        assertThat(existingTransactionOpt1).isPresent();
        assertThat(existingTransactionOpt2).isPresent();

        Transaction transaction1 = existingTransactionOpt1.get();
        transaction1.setAmount(3000.0); // Modify amount

        Transaction transaction2 = existingTransactionOpt2.get();
        transaction2.setAmount(2000.0); // Modify amount

        List<Transaction> transactionsToUpdate = List.of(transaction1, transaction2);

        // Act
        repository.saveInBatch(transactionsToUpdate, entityManager);

        List<Transaction> updatedTransactions = repository.findAll().stream().filter(t -> t.getTransactionId().equals("TX001") || t.getTransactionId().equals("TX002")).toList();

        // Assert
        assertThat(updatedTransactions).hasSize(2);

        for (Transaction updated : updatedTransactions) {
            assertThat(updated.getTimeOut()).isEqualTo(Temporal.INFINITY);
            assertThat(updated.getTimeIn()).isNotNull();
        }

        Optional<Transaction> updatedTransaction1Opt = updatedTransactions.stream().filter(t -> t.getTransactionId().equals("TX001")).findFirst();
        assertThat(updatedTransaction1Opt).isPresent();
        assertThat(updatedTransaction1Opt.get().getAmount()).isEqualTo(3000.0);
    }

    @Test
    void deleteInBatch_ShouldSoftDeleteMultipleTransactions() {
        // Given
        List<Transaction> transactions = Arrays.asList(
                activeTransaction1,
                activeTransaction2
        );

        // When
        repository.deleteInBatch(transactions);
        entityManager.flush();

        // Then
        for (Transaction transaction : transactions) {
            Transaction deletedTransaction = entityManager.find(Transaction.class, new TransactionId(timeIn, transaction.getTransactionId()));
            assertNotNull(deletedTransaction);
            assertNotEquals(Temporal.INFINITY, deletedTransaction.getTimeOut());
            assertTrue(deletedTransaction.getTimeOut() <= System.currentTimeMillis());
        }
    }

    @Test
    void deleteInBatchBatchSize_ShouldSoftDeleteMultipleTransactions() {
        // Given
        List<Transaction> transactions = Arrays.asList(
                activeTransaction1,
                activeTransaction2
        );

        // When
        repository.deleteInBatch(transactions, 1);
        entityManager.flush();

        // Then
        for (Transaction transaction : transactions) {
            Transaction deletedTransaction = entityManager.find(Transaction.class, new TransactionId(timeIn, transaction.getTransactionId()));
            assertNotNull(deletedTransaction);
            assertNotEquals(Temporal.INFINITY, deletedTransaction.getTimeOut());
            assertTrue(deletedTransaction.getTimeOut() <= System.currentTimeMillis());
        }
    }

    @Test
    void deleteInBatchBatchSize_ShouldSoftDeleteMultipleTransactionsWithEm() {
        // Given
        List<Transaction> transactions = Arrays.asList(
                activeTransaction1,
                activeTransaction2
        );

        // When
        repository.deleteInBatch(transactions, entityManager);
        entityManager.flush();

        // Then
        for (Transaction transaction : transactions) {
            Transaction deletedTransaction = entityManager.find(Transaction.class, new TransactionId(timeIn, transaction.getTransactionId()));
            assertNotNull(deletedTransaction);
            assertNotEquals(Temporal.INFINITY, deletedTransaction.getTimeOut());
            assertTrue(deletedTransaction.getTimeOut() <= System.currentTimeMillis());
        }
    }

    @Configuration
    @EnableJpaTemporalRepositories
    @EntityScan
    @EnableAutoConfiguration
    static class TestConfig {
    }

}


