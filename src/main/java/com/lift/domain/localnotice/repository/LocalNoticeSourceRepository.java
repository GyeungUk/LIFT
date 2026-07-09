package com.lift.domain.localnotice.repository;

import com.lift.domain.localnotice.model.LocalNoticeSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalNoticeSourceRepository extends JpaRepository<LocalNoticeSource, Long> {

    /** 활성화된 피드만 동기화 대상으로 조회한다. */
    List<LocalNoticeSource> findByEnabledTrue();
}
