package dev.yash.jpatemporal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base class for temporal entity identifiers.
 * <p>
 * This class is intended to be extended by ID classes of entities that represent objects
 * with temporal boundaries, specifically defined by a start time ({@code timeIn}).
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>Subclasses inheriting from this class must not redefine or modify the reserved {@code timeIn} field.</li>
 *     <li>Designed to be used in combination with temporal entities.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * {@code
 * public class ExampleTemporalEntityId extends TemporalId {
 *     // Add additional ID fields and methods here.
 * }
 * }
 * </pre>
 *
 * <p>
 * This class uses the Lombok library annotations {@code @Getter} and {@code @Setter}
 * to automatically generate boilerplate getter and setter methods.
 * </p>
 *
 * @author Yashwanth M
 */
@Getter
@Setter
@SuppressWarnings("checkstyle:MemberName")
public abstract class TemporalId {

    /**
     * The start time of the temporal entity, represented as a Unix timestamp in milliseconds.
     * This field is mapped to the database column {@code TIME_IN} and is mandatory (cannot be null).
     */
    @Id
    @Column(name = "TIME_IN", nullable = false)
    protected Long timeIn;

    /**
     * Default constructor.
     * <p>
     * This constructor is intentionally empty to allow subclasses to define their own
     * initialization logic if required.
     * </p>
     */
    protected TemporalId() {
        // No-op constructor
    }
}


