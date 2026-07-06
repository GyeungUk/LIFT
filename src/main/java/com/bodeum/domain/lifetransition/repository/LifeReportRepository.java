package com.bodeum.domain.lifetransition.repository;

import com.bodeum.domain.lifetransition.enumtype.PaymentStatus;
import com.bodeum.domain.lifetransition.model.LifeReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifeReportRepository extends JpaRepository<LifeReport, Long> {

    Optional<LifeReport> findByAssessment_Id(Long assessmentId);

    Optional<LifeReport> findFirstByAssessment_UserAccount_IdAndPaymentStatusOrderByPaidAtDescIdDesc(
            Long userId,
            PaymentStatus paymentStatus
    );

    Optional<LifeReport> findFirstByAssessment_UserAccount_IdOrderByIdDesc(Long userId);
}
