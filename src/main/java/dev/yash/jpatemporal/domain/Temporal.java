package dev.yash.jpatemporal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base class for temporal entities.
 * <p>
 * This class is intended to be extended by entities representing objects with
 * temporal boundaries, defined by a start time ({@code timeIn}) and an end time ({@code timeOut}).
 * It provides fields and constants to manage these temporal attributes effectively.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>The constant {@code INFINITY} is provided to represent an infinite or undefined end time.</li>
 *     <li>Subclasses extending this abstract class must not redefine or use the reserved fields {@code timeIn} and {@code timeOut}.</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * {@code
 * @Entity
 * public class ExampleTemporalEntity extends Temporal {
 *     // Define additional fields and methods here.
 * }
 * }
 * </pre>
 *
 * <p>
 * This class uses the Lombok library annotations {@code @Getter} and {@code @Setter} to
 * automatically generate getter and setter methods for its fields.
 * </p>
 *
 * @author Yashwanth M
 */
@Getter
@Setter
@MappedSuperclass
@SuppressWarnings("checkstyle:MemberName")
public abstract class Temporal {
    /**
     * Constant representing an infinite or undefined end time.
     * This is typically used when the temporal entity does not have a specific
     * end time or when the end time extends indefinitely.
     */
    public static final Long INFINITY = Long.MAX_VALUE;
    /**
     * Field name representing the start time (time-in) property.
     * This is useful for referring to the database column or property name programmatically.
     */
    public static final String TIME_IN_FIELD = "timeIn";
    /**
     * Field name representing the end time (time-out) property.
     * This is useful for referring to the database column or property name programmatically.
     */
    public static final String TIME_OUT_FIELD = "timeOut";
    /**
     * Start time of the temporal entity, represented as a Unix timestamp in milliseconds.
     * This field is mapped to the database column {@code TIME_IN} and cannot be null.
     */
    @Id
    @Column(name = "TIME_IN", nullable = false)
    private Long timeIn;
    /**
     * End time of the temporal entity, represented as a Unix timestamp in milliseconds.
     * This field is mapped to the database column {@code TIME_OUT} and cannot be null.
     */
    @Column(name = "TIME_OUT", nullable = false)
    private Long timeOut;

    /**
     * Default constructor.
     * This constructor is intentionally empty to allow subclasses to define
     * their own initialization logic if needed.
     */
    protected Temporal() {
        // No-op constructor
    }
}

