package com.community.goals.logic;

/**
 * Validates turn-in requests
 */
public class TurnInValidator {
    private static final long MIN_AMOUNT = 1;
    private static final long MAX_AMOUNT = Long.MAX_VALUE / 2; // Leave room for math operations

    /**
     * Validate if an amount is valid for turn-in
     */
    public boolean isValidAmount(long amount) {
        return amount >= MIN_AMOUNT && amount <= MAX_AMOUNT;
    }

    /**
     * Get minimum valid amount
     */
    public long getMinAmount() {
        return MIN_AMOUNT;
    }

    /**
     * Get maximum valid amount
     */
    public long getMaxAmount() {
        return MAX_AMOUNT;
    }
}
