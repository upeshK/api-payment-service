/**
 * @author ukulatunge
 * Created on 2 Dec 2025
 */

package com.up.payment.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.up.payment.model.Transaction;

/**
 * In-memory transaction replay detector
 */
public class TransactionReplayDetectorImpl implements TransactionReplayDetector {
	/**
	 * Store references key, timestamp for latest transactions
	 */
	private final ConcurrentMap<String, Instant> recentTransactions;
	/**
	 * Store transaction records in insertion order for time based cleanup
	 */
	private final ConcurrentLinkedQueue<TrackingRecord> transactionQueue;

	private static final Duration REPLAY_CHECKING_WINDOW = Duration.ofMinutes(10);

	private final Clock clock;

	private final Duration replayWindow;

	public TransactionReplayDetectorImpl() {
		this(REPLAY_CHECKING_WINDOW, Clock.systemUTC());
	}

	/**
	 * Creates a replay detector with custom replay window and clock.
	 * 
	 * @param replayWindow
	 * @param clock        for time calculations
	 */
	public TransactionReplayDetectorImpl(Duration replayWindow, Clock clock) {
		if (replayWindow == null || replayWindow.isZero() || replayWindow.isNegative()) {
			throw new IllegalArgumentException("replayWindow must be a positive duration");
		}
		this.replayWindow = replayWindow;
		this.clock = Objects.requireNonNull(clock, "Clock must not be null");
		this.recentTransactions = new ConcurrentHashMap<>();
		this.transactionQueue = new ConcurrentLinkedQueue<>();
	}

	@Override
	public boolean isReplay(Transaction transaction) {
		if (transaction != null) {
			Instant receivedTimestamp = recentTransactions.get(buildKey(transaction));
			Instant now = clock.instant();
			Instant cutOff = now.minus(replayWindow);
			return receivedTimestamp != null && !receivedTimestamp.isBefore(cutOff);
		}
		return false;
	}

	@Override
	public void register(Transaction transaction) {
		if (transaction != null) {
			Instant now = clock.instant();
			recentTransactions.put(buildKey(transaction), now);
			transactionQueue.add(new TrackingRecord(buildKey(transaction), now));

			// cleanup transactions received before Replay window
			Instant cutOff = now.minus(replayWindow);
			cleanup(cutOff);
		}

	}

	/**
	 * Time-Based cleanup remove transaction tracking records which are older than
	 * cutoff from tracking queue as well as
	 * 
	 * @param cutoff
	 */
	private void cleanup(Instant cutoff) {

		while (true) {
			TrackingRecord head = transactionQueue.peek();
			if (head == null || !head.timestamp().isBefore(cutoff)) {
				break;
			}
			// remove from queue
			transactionQueue.poll();
			// remove from map for given key only if timestamp matches
			recentTransactions.computeIfPresent(head.key,
					(key, timestamp) -> timestamp.equals(head.timestamp()) ? null : timestamp);

		}

	}

	/**
	 * Build composite semantic key This defines when two transactions are
	 * semantically equivalent.
	 * 
	 * @param txn
	 * @return composite key
	 */
	protected String buildKey(Transaction txn) {
		return String.join("|", txn.getTransactionId(), txn.getAmount().toPlainString(), txn.getCurrency());
	}

	/**
	 * Value object(Immutable) for insertion order tracking. Use in cleanup process
	 */
	private static record TrackingRecord(String key, Instant timestamp) {
	}

	public static void main(String[] args) {
		System.out.println();

		System.out.println("Hello");
		System.out.println("Hello");
	}

}
