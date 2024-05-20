package com.pundix.wallet.task.repository;

import com.pundix.wallet.task.entity.GatherUSDTRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GatherUSDTRecordRepository extends JpaRepository<GatherUSDTRecord, Integer> {

    @Query("SELECT g FROM GatherUSDTRecord g WHERE LOWER(g.status) = LOWER('pending')")
    List<GatherUSDTRecord> findPendingRecords();


}
