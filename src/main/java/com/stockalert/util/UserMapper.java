package com.stockalert.util;

import com.stockalert.common.UserDTO;
import com.stockalert.model.User;

public class UserMapper {
    public static UserDTO toDTO(User user) {
        return new UserDTO(user.getUserId(), user.getName(), user.getEmail(), user.getMobileno());
    }
}