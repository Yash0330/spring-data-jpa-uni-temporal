package dev.yash.jpatemporal.repository;

import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNullApi;

import java.util.List;
import java.util.Optional;

public interface TemporalCustomRepository<T, ID>  extends CrudRepository<T, ID> {
    @Override
    @Nonnull
    List<T> findAll();

    @Nonnull
    List<T> findAllByBatchSize(int batchSize);

    @Override
    @Nonnull
    Optional<T> findById(ID id);

    @Nonnull
    Optional<T> find(String predicate);
}
