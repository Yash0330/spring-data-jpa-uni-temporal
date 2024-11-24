package dev.yash.jpatemporal.repository;

import dev.yash.jpatemporal.domain.Transaction;
import dev.yash.jpatemporal.domain.TransactionId;

public interface TransactionRepository extends TemporalRepository<Transaction, TransactionId> {
}
