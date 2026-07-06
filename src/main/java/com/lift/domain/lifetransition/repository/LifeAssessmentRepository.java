package com.lift.domain.lifetransition.repository;

import com.lift.domain.lifetransition.model.LifeAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifeAssessmentRepository extends JpaRepository<LifeAssessment, Long> {
}
