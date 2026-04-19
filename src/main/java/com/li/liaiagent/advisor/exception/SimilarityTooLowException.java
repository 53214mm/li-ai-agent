package com.li.liaiagent.advisor.exception;

public class SimilarityTooLowException extends RuntimeException {
    public SimilarityTooLowException(String message) {
        super(message);
    }

    public SimilarityTooLowException(String message, Throwable cause) {
        super(message, cause);
    }
}
