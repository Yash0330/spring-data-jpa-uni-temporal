# Spring Data JPA Uni Temporal Audit
[![Maven Central](https://img.shields.io/maven-central/v/io.github.yash0330/spring-data-jpa-uni-temporal.svg?label=Maven%20Central&color=success)](https://central.sonatype.com/artifact/io.github.yash0330/spring-data-jpa-uni-temporal)
[![javadoc](https://javadoc.io/badge2/io.github.yash0330/spring-data-jpa-uni-temporal/javadoc.svg)](https://javadoc.io/doc/io.github.yash0330/spring-data-jpa-uni-temporal)

Spring Data JPA Uni Temporal Audit is an extension of [spring-data-jpa](https://github.com/spring-projects/spring-data-jpa) that makes it simple to keep an audit of your data in the same table as your main data itself.

It does that by implementing [temporal database](https://en.wikipedia.org/wiki/Temporal_database) functionality completely on the application side, so it works with any DB engines that JPA integrates with using minimal configuration.

It also provides batch operations for saving and deleting entities in batches.

More specifically, your table becomes a "system-version table". The following excerpt is from [mariadb](https://mariadb.com/docs/appdev/temporal-tables/#application-time-period-tables):

> System-Versioned Tables
>
> Normally, when you issue a statement that updates a row on a table, the new values replace the old values on the row so that only the most current data remains available to the application.
>
> With system-versioned tables, [the db server] tracks the points in time when rows change. When you update a row on these tables, it creates a new row to display as current without removing the old data. This tracking remains transparent to the application.
> When querying a system-versioned table, you can retrieve either the most current values for every row or the historic values available at a given point in time.
>
> You may find this feature useful in efficiently tracking the time of changes to continuously-monitored values that do not change frequently, such as changes in temperature over the course of a year. System versioning is often useful for auditing.

# Who is this for?

- You want a simple and lightweight auditing mechanism for your tables, i.e.:
    - No other tables are created
    - No triggers
    - Adding a new column automatically starts versioning it
    - Minimal springboot configuration
    - Want fast and efficient batch operations for temporal tables
- You want a no fuss way of looking at audit:
    - You can very easily use regular `find` methods to find data which is valid in the current time period
    - At a database level, everything is in the same table as your main data itself, so it's very easy to analyse, report and build upon that data.
- You have a relatively simple model and can live with the limitations of this library described in the [Limitations](#Limitations) section below.

# Usage

Look at the latest release `${version}` in github and add a dependency to your build file:

Maven:
```xml
<dependency>
    <groupId>io.github.yash0330</groupId>
    <artifactId>spring-data-jpa-Uni-temporal</artifactId>
    <version>${version}</version>
</dependency>
```

Gradle:
```groovy
implementation 'io.github.yash0330:spring-data-jpa-Uni-temporal:${version}'
```

As a quick-start, take a look at the sample tests in the [src/test/java](src/test/java) directory. It shows the simplest usage of this extension. For extra information and alternative usage here's a step-by-step summary of what you need:

### Spring main class 

Use `@EnableJpaTemporalRepositories`.
This makes this extension work, and that it _only_ works on repositories that extend `TemporalRepository.java` (see below).
Alternatively, you can use `@EnableJpaRepositories(repositoryFactoryBeanClass = DefaultRepositoryFactoryBean.class)` if you need to configure something else for your regular JPA repositories.

### Entity (e.g. [Transaction.java](src/test/java/dev/yash/jpatemporal/domain/Transaction.java))

From your domain class (e.g. `Transaction.java`), extend `Temporal.java`.

### Id class of Entity (e.g. [TransactionId.java](src/test/java/dev/yash/jpatemporal/domain/TransactionId.java))

From your IdClass (e.g. `TransactionId.java`) extend `TemporalId.java`.

### Repository (e.g. [TransactionRepository.java](src/test/java/dev/yash/jpatemporal/repository/TransactionRepository.java))

Create a repository interface that extends `TemporalRepository<T,ID>`. `T` is your entity and `ID` is the type of your unique key.
Example `extends TemporalRepository<Transaction, TransactionId>`

## Batch Operations

This library provides optimized batch operations for JPA entities, allowing you to efficiently save or delete large collections of entities in batches.

### Available Batch Methods

#### Save Operations
- **`saveInBatch(List<T>)`**  
  Save a list of entities in batch.
- **`saveAllInBatch(List<T>, EntityManager entityManager)`**  
  Save all entities using a specified `EntityManager`.
- **`saveInBatch(List<T>, int batchSize)`**  
  Save a list of entities with a custom batch size.

#### Delete Operations
- **`deleteInBatch(List<T>)`**  
  Delete a list of entities in batch.
- **`deleteAllInBatch(List<T>, EntityManager entityManager)`**  
  Delete all entities using a specified `EntityManager`.
- **`deleteInBatch(List<T>, int batchSize)`**  
  Delete a list of entities with a custom batch size.

### Optimized for Batch Processing

These methods are designed for performance and scalability, allowing you to process large datasets efficiently. For example:
- If you have a list of 1000 entities, you can use `saveInBatch` or `deleteInBatch` to handle them in chunks instead of processing all entities at once.

### Batch Size Configuration

The batch size determines the number of entities processed in a single batch. It can be configured in the following ways:

### Default Batch Size
If not explicitly set, the default batch size is **700**.

### Configuring Batch Size via Properties

#### Properties File
```properties
jpa.temporal.batchSize=700
```
#### YML File
```yml
jpa:
  temporal:
    batchSize:  700
```

## Parallel Batch Processing

The **`ParallelBatchUpdater`** class provides utilities for performing batch operations in parallel using multiple threads.

### Key Features
- Divides the list of entities into sublists and processes each sublist independently.
- Initializes a separate `EntityManager` for each thread to ensure thread safety.
- Optimized for parallel saving and deleting of entities.

### Example

```java
@Component
public class MyService {

    @Autowired
    private ParallelBatchUpdater parallelBatchUpdater;

    @Autowired
    private TemporalRepository<MyEntity, Long> repository;

    public void processEntities(List<MyEntity> entities) {
        int threads = 4; // Number of parallel threads
        parallelBatchUpdater.saveInParallel(entities, threads, repository);

        // For deletion:
        parallelBatchUpdater.deleteInParallel(entities, threads, repository);
    }
}
```

# Alternatives

- [Javers](https://github.com/javers/javers)
- [Envers](https://github.com/spring-projects/spring-data-envers)
- [Spring Data JPA Temporal](https://github.com/ClaudioConsolmagno/spring-data-jpa-temporal)
- A custom implementation with `@PrePersist`, `@PreUpdate`, etc.
- Triggers on the database (please don't...)
- A native temporal implementation to your database engine (e.g. [mariadb](https://mariadb.com/docs/appdev/temporal-tables/#application-time-period-tables)).
  This is a nice auditing solution, it's basically what this project does but at the DB level.
  This means it's not portable, you'll need to configure things manually and create manual audit finder methods.

# Limitations

The following 2 functionalities aren't currently supported with this library. An exception may be thrown at spring boot start-up if you try to use them. I'll try and work on those in the future.

- Does not support [derived query methods](https://www.baeldung.com/spring-data-derived-queries), e.g. `findByNameAndAddress`, `countByNameAndAddress`, etc. However, you could create methods and use `@Query` annotation to specify a query to run.
- Does not support relations, e.g. `@OneToOne`, `@OneToMany`, etc.

# Next Steps

- [ ] Add debug/trace logging
- [ ] Work on limitations (section above)
    - Maybe listeners can help solve relations limitation (e.g. see EnversPreUpdateEventListenerImpl
- [ ] Add support for associations
- [ ] Add support for querying a specific time period
- [ ] Mongodb support (separate library, same concept)

# License

Spring Data Uni JPA Temporal Audit is Open Source software released under the [Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0.html).
