package com.socialnet.exception;

public class EmailAlreadyPresentedException extends BadRequestException {
    public EmailAlreadyPresentedException(String message) {
        super(message);
    }
}
