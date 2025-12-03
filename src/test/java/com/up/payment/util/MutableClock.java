/**
 * @author ukulatunge
 * Created on 2 Dec 2025
 */

package com.up.payment.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Test utility clock
 */
public class MutableClock extends Clock {
	
	private Instant current;
	private final ZoneId zone;
	
	public MutableClock(Instant initial, ZoneId zone) {
        this.current = initial;
        this.zone = zone;
    }

	@Override
	public ZoneId getZone() {	
		return zone;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new MutableClock(current, zone);
	}

	@Override
    public Instant instant() {
        return current;
    }
	
	 public void advanceSeconds(long seconds) {
	        current = current.plusSeconds(seconds);
	    }

	    public void advance(Duration duration) {
	        current = current.plus(duration);
	    }

}
