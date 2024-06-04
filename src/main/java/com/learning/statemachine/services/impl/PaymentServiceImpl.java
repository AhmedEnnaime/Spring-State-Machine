package com.learning.statemachine.services.impl;

import com.learning.statemachine.domain.Payment;
import com.learning.statemachine.domain.PaymentEvent;
import com.learning.statemachine.domain.PaymentState;
import com.learning.statemachine.repositories.PaymentRepository;
import com.learning.statemachine.services.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    public static final String PAYMENT_ID_HEADER = "paymentId";
    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<PaymentState, PaymentEvent> stateMachineFactory;
    private final PaymentStateChangeInterceptor paymentStateChangeInterceptor;

    @Override
    public Payment createPayment(Payment payment) {
        payment.setState(PaymentState.NEW);
        return paymentRepository.save(payment);
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);
        sendEvent(paymentId, stateMachine, PaymentEvent.PRE_AUTHORIZED);
        return stateMachine;
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);
        sendEvent(paymentId, stateMachine, PaymentEvent.AUTHORIZE);
        return stateMachine;
    }

    @Transactional
    @Override
    public StateMachine<PaymentState, PaymentEvent> declineAuth(Long paymentId) {
        StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);
        sendEvent(paymentId, stateMachine, PaymentEvent.AUTHORIZE_DECLINED);
        return stateMachine;
    }

    private void sendEvent(Long paymentId, StateMachine<PaymentState, PaymentEvent> stateMachine, PaymentEvent paymentEvent) {
        Message message = MessageBuilder.withPayload(paymentEvent)
                .setHeader(PAYMENT_ID_HEADER, paymentId)
                .build();
        stateMachine.sendEvent(message);

    }

    private StateMachine<PaymentState, PaymentEvent> build(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        StateMachine<PaymentState, PaymentEvent> sm = stateMachineFactory.getStateMachine(Long.toString(payment.getId()));
        sm.stop();
        sm.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.addStateMachineInterceptor(paymentStateChangeInterceptor);
            sma.resetStateMachine(new DefaultStateMachineContext<>(payment.getState(), null, null, null));
        });

        sm.start();
        return sm;
    }
}
