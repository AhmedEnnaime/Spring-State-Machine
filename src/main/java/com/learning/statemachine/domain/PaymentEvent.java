package com.learning.statemachine.domain;

public enum PaymentEvent {
    PRE_AUTHORIZED,
    PRE_AUTH_APPROVED,
    PRE_AUTH_DECLINED,
    AUTHORIZE,
    AUTHORIZE_APPROVED,
    AUTHORIZE_DECLINED,
}
