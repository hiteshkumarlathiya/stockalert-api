package com.stockalert.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.stockalert.model.User;
import com.stockalert.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserDataSeeder implements CommandLineRunner {

    @Autowired
    private DummyUserGenerator dummyUserGenerator;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 1 || userRepository.count() == 0) {
            List<User> users = dummyUserGenerator.generateUsers(1000);
            userRepository.saveAll(users);
            log.info("Seeded 1000 dummy users into the database.");
        }
    }
}
