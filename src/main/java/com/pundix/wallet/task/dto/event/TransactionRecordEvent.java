package com.pundix.wallet.task.dto.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import org.web3j.protocol.core.methods.response.Transaction;

@Getter
@Setter
public class TransactionRecordEvent extends ApplicationEvent {

    private Transaction transaction;

    public TransactionRecordEvent(Object source, Transaction transaction) {
        super(source);
        this.transaction = transaction;
    }
}
