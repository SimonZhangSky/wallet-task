package com.pundix.wallet.task.repository;

import com.pundix.wallet.task.entity.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.List;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Integer> {

    @Query("SELECT g FROM TransactionRecord g WHERE LOWER(g.status) = LOWER('pending')")
    List<TransactionRecord> findPendingRecords();

    @Query("SELECT tr FROM TransactionRecord tr " +
            "WHERE (tr.fromAddress = :fromAddress or tr.toAddress = :fromAddress) " +
            "AND (:startBlock IS NULL OR tr.blockNumber >= :startBlock) " +
            "AND (:endBlock IS NULL OR tr.blockNumber <= :endBlock) ")
    Page<TransactionRecord> findByFilters(
            @Param("fromAddress") String fromAddress,
            @Param("startBlock") BigInteger startBlock,
            @Param("endBlock") BigInteger endBlock,
            Pageable pageable);

}
