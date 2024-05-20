package com.pundix.wallet.task.repository;

import com.pundix.wallet.task.entity.GatherGasRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GatherGasRecordRepository extends JpaRepository<GatherGasRecord, Integer> {

    @Query("SELECT g FROM GatherGasRecord g WHERE LOWER(g.status) = LOWER('pending')")
    List<GatherGasRecord> findPendingRecords();


}
