package com.file.storage.service;

import com.file.storage.dto.UserResponse;
import com.file.storage.exceptions.UnauthorizedUserException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public UserResponse getMe(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedUserException();
        }
        return new UserResponse(userDetails.getUsername());
    }
}
