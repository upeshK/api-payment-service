/**
 * @author ukulatunge
 * Created on 2 Dec 2025
 */

package com.up.payment.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.up.payment.model.Transaction;

/**
 * 
 */
class TransactionReplayDetectorImplTest {

		
	private Transaction newTransaction(String txnId, Instant now) {
		return new Transaction(txnId, "Account01", "Credit", new BigDecimal("220.24"),"GBP", now);
	}
	
	@Test
	void registerReplayTransaction_whenReceiveFirst_noReplayDetect_secondRecived_replayDetect() {
		MutableClock clock = new MutableClock(Instant.parse("2025-12-02T15:00:00Z"), ZoneOffset.UTC);
		TransactionReplayDetector replayDetector=new TransactionReplayDetectorImpl(Duration.ofMinutes(10),clock);
		
		Transaction transaction=newTransaction("tx01",clock.instant());		
		assertFalse(replayDetector.isReplay(transaction), "First time received transaction should not be replay");
		replayDetector.register(transaction);
		
		assertTrue(replayDetector.isReplay(transaction), "Second time received transaction within replay window should be replay");		
		
	}
	
	
	@Test
	void registerReplayTransaction_whenRecivedAgainAfterReplayWindow_replayDetect() {
		
		MutableClock clock = new MutableClock(Instant.parse("2025-12-02T15:00:00Z"), ZoneOffset.UTC);
		TransactionReplayDetector replayDetector=new TransactionReplayDetectorImpl(Duration.ofMinutes(10),clock);
		
		Transaction transaction=newTransaction("tx02",clock.instant());	
		replayDetector.register(transaction);
		
		clock.advance(Duration.ofMinutes(9)); // increment by 9 minutes
		assertTrue(replayDetector.isReplay(transaction), "Still in side replay window");
		
		
		clock.advance(Duration.ofMinutes(2)); // increment by another 2 minutes
		assertFalse(replayDetector.isReplay(transaction), "After 10 minutes(11 minutes) is out older that replay window, so should not be replay");
		
		
	}
	
	@Test
	void registerReplayTransaction_whenRecivedDifferentAmount_noReplayDetect() {
		//arrange
		MutableClock clock = new MutableClock(Instant.parse("2025-12-02T15:00:00Z"), ZoneOffset.UTC);
		TransactionReplayDetector replayDetector=new TransactionReplayDetectorImpl(Duration.ofMinutes(10),clock);
		
		Transaction tx1=new Transaction("tx03", "Account01", "Credit", new BigDecimal("220.24"),"GBP", clock.instant());
		Transaction tx2=new Transaction("tx03", "Account01", "Credit", new BigDecimal("230.24"),"GBP", clock.instant());			
		//perform
		replayDetector.register(tx1);
		//perform & assert
		assertFalse(replayDetector.isReplay(tx2), "Different amount should not be replay");	
		
	}
	
	@Test
	void registerReplayTransaction_whenRecivedDifferentAccounttype_replayDetect() {
		//arrange
		MutableClock clock = new MutableClock(Instant.parse("2025-12-02T15:00:00Z"), ZoneOffset.UTC);
		TransactionReplayDetector replayDetector=new TransactionReplayDetectorImpl(Duration.ofMinutes(10),clock);
		
		Transaction tx1=new Transaction("tx03", "Account01", "Credit", new BigDecimal("220.24"),"GBP", clock.instant());
		Transaction tx2=new Transaction("tx03", "Account01", "Loan", new BigDecimal("220.24"),"GBP", clock.instant());			
		//perform
		replayDetector.register(tx1);
		//perform & assert
		assertTrue(replayDetector.isReplay(tx2), "Different amount should not be replay");	
		
	}
	
	@Test
	void concurrentRegisterAndRepaly() throws InterruptedException {
		
		MutableClock clock = new MutableClock(Instant.parse("2025-12-02T15:00:00Z"), ZoneOffset.UTC);
		TransactionReplayDetector replayDetector=new TransactionReplayDetectorImpl(Duration.ofMinutes(10),clock);
		Transaction transaction=newTransaction("tx02",clock.instant());	
		
		
		int threads =20;
		
		Thread[] workers = new Thread[threads];
		
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new Thread(()-> {
				if(Math.random() <0.5) {
					replayDetector.register(transaction);
				}else {
					replayDetector.isReplay(transaction);
				}
			});
			
		}
		
		for (Thread t : workers) {
			t.start();
		}
		
		for (Thread t : workers) {
			t.join();
		}
		
		
		 assertTrue(replayDetector.isReplay(transaction));
		
	}
	
	@Test
	void cleanupRemovesExpiredEntries() {
		
		MutableClock clock = new MutableClock(Instant.parse("2025-12-02T15:00:00Z"), ZoneOffset.UTC);
	    TransactionReplayDetector replayDetector = new TransactionReplayDetectorImpl(Duration.ofMinutes(10), clock);

	    // register first transaction at t0
	    Transaction t1 = newTransaction("tx-clean-1", clock.instant());
	    replayDetector.register(t1);

	    // advance 5 minutes and register second transaction at t0+5
	    clock.advance(Duration.ofMinutes(5));
	    Transaction t2 = newTransaction("tx-clean-2", clock.instant());
	    replayDetector.register(t2);

	    // advance another 6 minutes -> now t1 is 11 minutes old (expired), t2 is 6 minutes old (still valid)
	    clock.advance(Duration.ofMinutes(6));

	    // advance another 10 minutes -> now t1 is 21 minutes old (expired), t2 is 16 minutes old (expired), t3 is 10 minutes old(still valid)
	    clock.advance(Duration.ofMinutes(10));
	    Transaction t3 = newTransaction("tx-clean-3", clock.instant());
	    replayDetector.register(t3);// this should clean up all the expired entries

	    assertFalse(replayDetector.isReplay(t1), "Expired entry t1 should be removed by cleanup");
	    assertFalse(replayDetector.isReplay(t2), "Expired entry t2 should be removed by cleanup");
	    assertTrue(replayDetector.isReplay(t3), "Recent entry t3 should still be considered a replay");
	}
	
	@Test
	void sameTransactionRecivedAfterExpiredThePrevious_thenUpdatestimestampToLatestAndcleanupOldestOne() {
		
		MutableClock clock = new MutableClock(Instant.parse("2025-12-02T15:00:00Z"), ZoneOffset.UTC);
	    TransactionReplayDetector replayDetector = new TransactionReplayDetectorImpl(Duration.ofMinutes(10), clock);

	    // register first transaction at t0
	    Transaction t1 = newTransaction("tx-clean-1", clock.instant());
	    replayDetector.register(t1);
	   
	    // advance 12 minutes and register same transaction again at t0+12
	    clock.advance(Duration.ofMinutes(12));
	  //  Transaction t2 = newTransaction("tx-clean-1", clock.instant());
	    replayDetector.register(t1);
	 // advance 5 minutes and replay same transaction again , now it is old only 5 minutes
	    clock.advance(Duration.ofMinutes(5));
	    assertTrue(replayDetector.isReplay(t1), "Entry t1's timestamp has updated so  should still be considered a replay");
	    

	}
	
	 @Test
	    void showsRaceBetweenIsReplayAndRegister() throws Exception {
	        // Use a fixed clock so time is stable
	        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T10:00:00Z"),ZoneOffset.UTC);
	        TransactionReplayDetector detector = new TransactionReplayDetectorImpl(Duration.ofMinutes(10), clock);

	        Transaction txn = newTransaction("race-id", clock.instant());

	        AtomicInteger processedCount = new AtomicInteger(0);

	        // Barrier to ensure both threads complete isReplay() before either calls register()
	        CyclicBarrier barrier = new CyclicBarrier(2);

	        Runnable worker = () -> {
	            boolean replay = detector.isReplay(txn); // non-atomic check

	            // We expect both threads to see replay == false here
	            assertFalse(replay, "Both threads should initially see not-replay");

	            try {
	                // Wait for the other thread to also finish isReplay
	                barrier.await();
	            } catch (Exception e) {
	                throw new RuntimeException(e);
	            }

	            if (!replay) {
	                // Simulate "processing" the transaction
	                processedCount.incrementAndGet();
	                detector.register(txn); // non-atomic register
	            }
	        };

	        Thread t1 = new Thread(worker);
	        Thread t2 = new Thread(worker);

	        t1.start();
	        t2.start();

	        t1.join();
	        t2.join();

	        // Because of the race, both threads "processed" the transaction
	        assertEquals(2, processedCount.get(),"Both threads processed the transaction due to the race between isReplay and register");
	    }
	}
	
	


