package com.recred.backend.domain.services;

import com.recred.backend.domain.repositories.TutorRepository;

public class TutorService {

    private TutorRepository tutorRepository;

    public TutorService(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }
}
