package dev.yash.jpatemporal.annotation;

import dev.yash.jpatemporal.repository.TemporalRepository;
import dev.yash.jpatemporal.repository.impl.TemporalRepositoryImpl;
import dev.yash.jpatemporal.support.DefaultRepositoryFactoryBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable JPA repositories and the TemporalRepository extension. Will scan the package of the annotated
 * configuration class for Spring Data repositories by default.
 * <p>
 * Repositories extending {@link TemporalRepository} will use implementation from {@link TemporalRepositoryImpl} whereas
 * other repositories will use their default implementation (e.g. {@link JpaRepository} has implementation from
 * {@link JpaRepositoryImplementation}).
 *
 * @see TemporalRepository
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableJpaRepositories(repositoryFactoryBeanClass = DefaultRepositoryFactoryBean.class)
public @interface EnableJpaTemporalRepositories {
}
