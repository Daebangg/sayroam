package com.daebangg.sayroam.settlement.domain;

import com.daebangg.sayroam.common.domain.Money;
import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "expense_share",
        uniqueConstraints = @UniqueConstraint(name = "uk_expense_share_expense_user", columnNames = {"expense_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseShare extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Embedded
    private Money amount;

    // Expense.addShare로만 만든다
    ExpenseShare(Expense expense, Long userId, Money amount) {
        this.expense = expense;
        this.userId = userId;
        this.amount = amount;
    }

}
