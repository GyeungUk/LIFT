package com.bodeum.domain.lifetransition.repository;

import com.bodeum.domain.lifetransition.model.LifeAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifeAssessmentRepository extends JpaRepository<LifeAssessment, Long> {
}
