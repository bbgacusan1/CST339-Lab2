package edu.gcu.cst339.lab2_chinook_api.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException extends RuntimeException {
    
    UserNotFoundException(Integer id) {
        super("User not found with ID: " + id);
    }
}
