package com.recred.backend.domain.services;

import com.recred.backend.domain.models.TutorInvitation;

public interface TutorInvitationEmailSender {

    void sendInvitation(TutorInvitation invitation, String invitationLink);
}
