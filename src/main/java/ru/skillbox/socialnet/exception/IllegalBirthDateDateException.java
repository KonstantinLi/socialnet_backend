package ru.skillbox.socialnet.exception;

public class IllegalBirthDateDateException extends BadRequestException {
    public IllegalBirthDateDateException(String message) {
        super(message);
    }
}
