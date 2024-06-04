package com.learning.statemachine.services.impl;

import com.learning.statemachine.domain.Payment;
import com.learning.statemachine.domain.PaymentEvent;
import com.learning.statemachine.domain.PaymentState;
import com.learning.statemachine.repositories.PaymentRepository;
import com.learning.statemachine.services.PaymentService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentServiceImplTest {

    @Autowired
    PaymentService paymentService;
    @Autowired
    PaymentRepository paymentRepository;
    Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .amount(new BigDecimal("100.00"))
                .build();
    }

    @Transactional
    @Test
    void preAuth() {
        Payment savedPayment = paymentService.createPayment(payment);
        System.out.println("Initial State: " + savedPayment.getState());
        paymentService.preAuth(savedPayment.getId());
        StateMachine<PaymentState, PaymentEvent> sm = paymentService.preAuth(payment.getId());
        Payment preAuthedPayment = paymentRepository.findById(savedPayment.getId()).get();
        System.out.println(sm.getState().getId());
        System.out.println(preAuthedPayment);
    }
}