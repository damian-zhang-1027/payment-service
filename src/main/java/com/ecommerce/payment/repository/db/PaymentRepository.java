package com.ecommerce.payment.repository.db;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.payment.model.db.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
