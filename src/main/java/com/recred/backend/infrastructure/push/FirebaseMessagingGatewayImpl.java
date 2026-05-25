package com.recred.backend.infrastructure.push;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class FirebaseMessagingGatewayImpl implements FirebaseMessagingGateway {

    @Override
    public void send(FirebaseApp firebaseApp, Message message) throws FirebaseMessagingException {
        FirebaseMessaging.getInstance(firebaseApp).send(message);
    }
}
