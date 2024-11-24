package dev.yash.jpatemporal.support;

import dev.yash.jpatemporal.repository.TemporalRepository;
import dev.yash.jpatemporal.repository.impl.TemporalRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Adapter to setup implementation {@link TemporalRepositoryImpl} against interface {@link TemporalRepository}.
 * Repositories that don't extend {@link TemporalRepository} use instead whatever implementation is defined by the base
 * class {@link JpaRepositoryFactoryBean} (usually {@link SimpleJpaRepository})
 *
 * @param <T> the type of the repository
 * @param <S>  The type of the entity the repository manages.
 * @param <ID> The type of the identifier of the entity the repository manages.
 */
public class DefaultRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
        extends JpaRepositoryFactoryBean<T, S, ID> {

    private final boolean isTemporalRepository;

    /**
     * Constructs a new {@code DefaultRepositoryFactoryBean}.
     *
     * @param repositoryInterface The repository interface class to be implemented.
     */
    public DefaultRepositoryFactoryBean(final Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
        this.isTemporalRepository = TemporalRepository.class.isAssignableFrom(repositoryInterface);
    }

    @NonNull
    @Override
    protected RepositoryFactorySupport createRepositoryFactory(@NonNull final EntityManager entityManager) {
        return isTemporalRepository
                ? new DefaultRepositoryFactory(entityManager)
                : super.createRepositoryFactory(entityManager);
    }

    static class DefaultRepositoryFactory extends JpaRepositoryFactory {
        DefaultRepositoryFactory(final EntityManager entityManager) {
            super(entityManager);
        }

        @NonNull
        @Override
        protected Class<?> getRepositoryBaseClass(final @NonNull RepositoryMetadata metadata) {
            return TemporalRepositoryImpl.class;
        }

        @NonNull
        @Override
        protected Optional<QueryLookupStrategy> getQueryLookupStrategy(final QueryLookupStrategy.Key key, @NonNull final QueryMethodEvaluationContextProvider evaluationContextProvider) {
            return super.getQueryLookupStrategy(QueryLookupStrategy.Key.USE_DECLARED_QUERY, evaluationContextProvider);
        }
    }
}
