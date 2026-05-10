package com.rc1.mantra_svc.model.chat;

/**
 * Delivery/read state of a message from the sender's perspective.
 */
public enum MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
