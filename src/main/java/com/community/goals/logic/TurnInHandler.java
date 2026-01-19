package com.community.goals.logic;

import com.community.goals.Goal;

/**
 * Handles turn-in of progress (when players submit proof of progress)
 */
public class TurnInHandler {
    private final GoalProgressTracker tracker;
    private final TurnInValidator validator;

    public TurnInHandler(GoalProgressTracker tracker) {
        this.tracker = tracker;
        this.validator = new TurnInValidator();
    }

    /**
     * Process a turn-in request for a goal
     */
    public TurnInResult processTurnIn(String goalId, long amount, String submitterName) {
        // Validate goal exists
        Goal goal = tracker.getGoal(goalId);
        if (goal == null) {
            return TurnInResult.fail("Goal not found: " + goalId);
        }

        // Validate goal is active
        if (goal.isCompleted()) {
            return TurnInResult.fail("Goal is already completed");
        }

        // Validate amount
        if (!validator.isValidAmount(amount)) {
            return TurnInResult.fail("Invalid progress amount: " + amount);
        }

        // Process the turn-in
        try {
            long previousProgress = goal.getCurrentProgress();
            tracker.addProgress(goalId, amount);
            
            boolean goalCompleted = goal.isCompleted();
            long newProgress = goal.getCurrentProgress();
            
            return TurnInResult.success(
                submitterName,
                goalId,
                amount,
                previousProgress,
                newProgress,
                goal.getTargetProgress(),
                goalCompleted
            );
        } catch (Exception e) {
            return TurnInResult.fail("Error processing turn-in: " + e.getMessage());
        }
    }

    /**
     * Validate a turn-in before processing
     */
    public ValidationResult validateTurnIn(String goalId, long amount) {
        Goal goal = tracker.getGoal(goalId);
        if (goal == null) {
            return ValidationResult.invalid("Goal not found");
        }

        if (goal.isCompleted()) {
            return ValidationResult.invalid("Goal is already completed");
        }

        if (!validator.isValidAmount(amount)) {
            return ValidationResult.invalid("Invalid amount");
        }

        return ValidationResult.valid();
    }

    /**
     * Result of a turn-in operation
     */
    public static class TurnInResult {
        private final boolean success;
        private final String message;
        private final String submitter;
        private final String goalId;
        private final long amountAdded;
        private final long previousProgress;
        private final long newProgress;
        private final long targetProgress;
        private final boolean goalCompleted;

        private TurnInResult(boolean success, String message, String submitter, String goalId,
                            long amountAdded, long previousProgress, long newProgress,
                            long targetProgress, boolean goalCompleted) {
            this.success = success;
            this.message = message;
            this.submitter = submitter;
            this.goalId = goalId;
            this.amountAdded = amountAdded;
            this.previousProgress = previousProgress;
            this.newProgress = newProgress;
            this.targetProgress = targetProgress;
            this.goalCompleted = goalCompleted;
        }

        public static TurnInResult success(String submitter, String goalId, long amountAdded,
                                          long previousProgress, long newProgress,
                                          long targetProgress, boolean goalCompleted) {
            return new TurnInResult(true, "Turn-in processed successfully", submitter, goalId,
                    amountAdded, previousProgress, newProgress, targetProgress, goalCompleted);
        }

        public static TurnInResult fail(String message) {
            return new TurnInResult(false, message, null, null, 0, 0, 0, 0, false);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getSubmitter() {
            return submitter;
        }

        public String getGoalId() {
            return goalId;
        }

        public long getAmountAdded() {
            return amountAdded;
        }

        public long getPreviousProgress() {
            return previousProgress;
        }

        public long getNewProgress() {
            return newProgress;
        }

        public long getTargetProgress() {
            return targetProgress;
        }

        public boolean isGoalCompleted() {
            return goalCompleted;
        }

        public double getProgressPercentage() {
            if (targetProgress <= 0) return 0;
            return (double) newProgress / targetProgress * 100;
        }
    }

    /**
     * Validation result for turn-in requests
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}
