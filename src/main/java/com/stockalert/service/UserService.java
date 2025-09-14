package com.stockalert.service;

import java.util.List;

import com.stockalert.model.User;

public interface UserService {
    User createUser(User user);
    User getUserById(String userId);
    List<User> getAllUsers();
    User updateUser(String userId, User user);
    void deleteUser(String userId);
}