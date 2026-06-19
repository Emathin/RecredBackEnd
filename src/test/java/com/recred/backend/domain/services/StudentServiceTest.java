package com.recred.backend.domain.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StudentServiceTest {

    @Test
    void implementsDomainStudentServiceContract() {
        assertInstanceOf(com.recred.backend.domain.StudentService.class, new StudentService());
    }
}
