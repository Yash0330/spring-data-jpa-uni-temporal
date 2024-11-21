package dev.yash.jpatemporal.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@ToString
public class Transaction extends Temporal {
    @Id
    private String transactionId;
    private String accountId;
    private String transactionType;
    private double amount;
}
