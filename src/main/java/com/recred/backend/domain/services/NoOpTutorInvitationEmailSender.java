package com.recred.backend.domain.services;

import com.recred.backend.domain.models.TutorInvitation;

public class NoOpTutorInvitationEmailSender implements TutorInvitationEmailSender {

    @Override
    public void sendInvitation(TutorInvitation invitation, String invitationLink) {
    }
}
