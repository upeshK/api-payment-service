/**
 * @author ukulatunge
 * Created on 1 Dec 2025
 */

package com.up.payment.util;

import com.up.payment.model.Transaction;

/**
 * Defines operations for managing in-memory transaction replay detector
 */
public interface TransactionReplayDetector {
	
	/**
	 * Checks if a transaction is a replay based on the current state
	 * @param txn
	 * @return true if a semantically equivalent txn was seen within the replay window
	 */
	public boolean isReplay(Transaction txn);
	
	/**
	 * Register a transaction if it is a not a replay
	 * @param txn
	 */
	public void register(Transaction txn);

}
