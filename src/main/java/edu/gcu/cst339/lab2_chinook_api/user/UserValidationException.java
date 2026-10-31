package edu.gcu.cst339.lab2_chinook_api.user;

class UserValidationException extends RuntimeException {

    UserValidationException(String message) {
        super(message);
    }
}
