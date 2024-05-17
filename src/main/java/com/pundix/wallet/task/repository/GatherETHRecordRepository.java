package com.pundix.wallet.task.repository;

import com.pundix.wallet.task.entity.GatherETHRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GatherETHRecordRepository extends JpaRepository<GatherETHRecord, Integer> {

    @Query("SELECT g FROM GatherETHRecord g WHERE LOWER(g.status) = LOWER('pending')")
    List<GatherETHRecord> findPendingRecords();


}
