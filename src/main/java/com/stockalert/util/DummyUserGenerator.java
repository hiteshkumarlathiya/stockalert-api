package com.stockalert.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.stockalert.model.User;

@Component
public class DummyUserGenerator {

    public List<User> generateUsers(int count) {
        List<User> users = new ArrayList<>();
        Random random = new Random();
        String[] firstNames = {"John", "Jane", "Alex", "Emily", "Chris", "Nina", "Raj", "Priya", "Sam", "Asha", "Kiran", "Pankaj", "Benson", "Kirti", "Divya", "Prabha", "Ashok"};
        String[] lastNames = {"Smith", "Doe", "Brown", "Patel", "Kumar", "Sharma", "Lee", "Singh", "Taylor", "Gupta", "Narwal", "Pandit", "Pandey", "Gokhle", "Pandya"};

        for (int i = 1; i <= count; i++) {
            String userId = String.format("USR%04d", i);
            String name = firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)];
            String email = "user" + String.format("%04d", i) + "@example.com";
            String mobileno = String.valueOf(7000000000L + random.nextInt(1000000000));

            users.add(new User(userId, name, email, mobileno));
        }
        return users;
    }
}
