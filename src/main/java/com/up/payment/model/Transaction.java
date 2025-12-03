/**
 * @author ukulatunge
 * Created on 1 Dec 2025
 */

package com.up.payment.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 
 */
public class Transaction {
	
	private final String transactionId;
	private final String fromAccount;
	private final String accountType;
	private final BigDecimal amount;
	private final String currency;
	private final Instant createdAt;
	
	
	public Transaction(String transactionId, String fromAccount, String accountType, BigDecimal amount,String currency,
			Instant createdAt) {
		super();
		this.transactionId = transactionId;
		this.fromAccount = fromAccount;
		this.accountType = accountType;
		this.amount = amount;
		this.currency = currency;
		this.createdAt = createdAt;
	}


	public String getTransactionId() {
		return transactionId;
	}


	public String getFromAccount() {
		return fromAccount;
	}


	public String getAccountType() {
		return accountType;
	}


	public BigDecimal getAmount() {
		return amount;
	}


	public Instant getCreatedAt() {
		return createdAt;
	}


	public String getCurrency() {
		return currency;
	}
	
	
	

}
