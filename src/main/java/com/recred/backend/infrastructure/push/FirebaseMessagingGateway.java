package com.recred.backend.infrastructure.push;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

public interface FirebaseMessagingGateway {
    void send(FirebaseApp firebaseApp, Message message) throws FirebaseMessagingException;
}
