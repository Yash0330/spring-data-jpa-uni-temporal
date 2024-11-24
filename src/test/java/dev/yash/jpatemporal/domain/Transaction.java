package dev.yash.jpatemporal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"transactionId", "TIME_OUT"}),
        }
)
@Getter
@Setter
@ToString
@IdClass(TransactionId.class)
public class Transaction extends Temporal {
    @Id
    private String transactionId;
    private String accountId;
    private String transactionType;
    private double amount;
}
