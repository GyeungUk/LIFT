package com.bodeum.domain.lifetransition.repository;

import com.bodeum.domain.lifetransition.model.ReportChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportChatMessageRepository extends JpaRepository<ReportChatMessage, Long> {

    List<ReportChatMessage> findByReport_IdOrderByIdAsc(Long reportId);
}
