package com.learning.statemachine.config;

import com.learning.statemachine.domain.PaymentEvent;
import com.learning.statemachine.domain.PaymentState;
import com.learning.statemachine.services.impl.PaymentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;
import java.util.Random;

@Slf4j
@EnableStateMachineFactory
@Configuration
public class StateMachineConfig extends StateMachineConfigurerAdapter<PaymentState, PaymentEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states) throws Exception {
        states.withStates()
                .initial(PaymentState.NEW)
                .states(EnumSet.allOf(PaymentState.class))
                .end(PaymentState.AUTH)
                .end(PaymentState.PRE_AUTH_ERROR)
                .end(PaymentState.AUTH_ERROR);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
        transitions.withExternal()
                .source(PaymentState.NEW).target(PaymentState.NEW).event(PaymentEvent.PRE_AUTHORIZED).action(preAuthAction()).guard(paymentIdGuard())
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH).event(PaymentEvent.PRE_AUTH_APPROVED)
                .and()
                .withExternal().source(PaymentState.NEW).target(PaymentState.PRE_AUTH_ERROR).event(PaymentEvent.PRE_AUTH_DECLINED)
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.PRE_AUTH).event(PaymentEvent.AUTHORIZE).action(authAction())
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH).event(PaymentEvent.AUTHORIZE_APPROVED)
                .and()
                .withExternal().source(PaymentState.PRE_AUTH).target(PaymentState.AUTH_ERROR).event(PaymentEvent.AUTHORIZE_DECLINED);
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<PaymentState, PaymentEvent> config) throws Exception {
        StateMachineListenerAdapter<PaymentState, PaymentEvent> adapter = new StateMachineListenerAdapter<PaymentState, PaymentEvent>() {
            @Override
            public void stateChanged(State<PaymentState, PaymentEvent> from, State<PaymentState, PaymentEvent> to) {
                log.info(String.format("State changed from %s to %s", from, to));
            }
        };
        config.withConfiguration().listener(adapter);
    }

    public Guard<PaymentState, PaymentEvent> paymentIdGuard() {
        return stateContext -> stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER) != null;
    }

    public Action<PaymentState, PaymentEvent> preAuthAction() {
        return stateContext -> {
            System.out.println("Pre Auth was called");
            if (new Random().nextInt(10) < 8 ) {
                System.out.println("Approved");
                stateContext.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_APPROVED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build());
            }else {
                System.out.println("Declined");
                stateContext.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_DECLINED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build());
            }
        };
    }

    public Action<PaymentState, PaymentEvent> authAction() {
        return stateContext -> {
            System.out.println("Auth was called");
            if (new Random().nextInt(10) < 8 ) {
                System.out.println("Auth Approved");
                stateContext.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.AUTHORIZE_APPROVED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build());
            }else {
                System.out.println("Auth Declined");
                stateContext.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.AUTHORIZE_DECLINED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build());
            }
        };
    }
}
