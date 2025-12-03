/**
 * @author ukulatunge
 * Created on 2 Dec 2025
 */

package com.up.payment.service;

import java.util.concurrent.ConcurrentHashMap;

import com.up.payment.model.Transaction;
import com.up.payment.util.TransactionReplayDetector;

/**
 * 
 */
public class PaymentService {
	
	private final TransactionReplayDetector transactionReplayDetector;
	 private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
	
	public PaymentService(TransactionReplayDetector replayDetector) {
		this.transactionReplayDetector = replayDetector;
	}
	
	public void registerPayment(Transaction transaction) {
		String id = transaction.getTransactionId();
		Object lock = locks.computeIfAbsent(id, k -> new Object());
		
		synchronized (lock) {
			if(transactionReplayDetector.isReplay(transaction)) {
				//handle replay transactions
			}else {
				//register new transactions
				transactionReplayDetector.register(transaction);
			}
		}
		
		  // remove the lock if it's still the same instance to avoid memory growth
        locks.remove(id, lock);
		
	}

}
