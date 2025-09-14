package com.stockalert.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.stockalert.model.User;

public interface UserRepository extends JpaRepository<User, String> {
}