package dev.yash.jpatemporal.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@AllArgsConstructor
public class TransactionId extends TemporalId implements Serializable  {
    private String transactionId; // Matches the @Id field in Transaction

    public TransactionId() {
    }

    public TransactionId(long timeIn, String transactionId) {
        this.timeIn = timeIn;
        this.transactionId = transactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}

