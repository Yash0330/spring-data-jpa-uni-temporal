package dev.yash.jpatemporal.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@AllArgsConstructor
public class TransactionId implements Serializable {
    private Long timeIn;          // Matches the inherited @Id field in Temporal
    private String transactionId; // Matches the @Id field in Transaction

    public TransactionId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return Objects.equals(transactionId, that.transactionId) &&
                Objects.equals(timeIn, that.timeIn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, timeIn);
    }
}

