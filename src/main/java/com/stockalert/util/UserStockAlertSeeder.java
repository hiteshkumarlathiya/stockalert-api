package com.stockalert.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.stockalert.common.NSEStockSymbols;
import com.stockalert.common.NotificationChannel;
import com.stockalert.common.TriggerType;
import com.stockalert.model.UserStockAlert;
import com.stockalert.repository.UserRepository;
import com.stockalert.repository.UserStockAlertRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserStockAlertSeeder implements CommandLineRunner {

    private static final int TARGET_ALERTS = 10_000;
    private static final int BATCH_SIZE = 1_000;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserStockAlertRepository alertRepository;
    
    private Map<String, Set<Double>> usedThresholds = new HashMap<>();

    private final Random rnd = new SecureRandom();

    @Override
    @Transactional
    public void run(String... args) {
        if (alertRepository.count() > 0) {
            log.info("UserStockAlert seeding skipped (alerts already present).");
            return;
        }

        // 1) Fetch up to ~300 userIds from your DB
        List<String> userIds = fetchUserIds(300);
        if (userIds.isEmpty()) {
            log.info("No users found for seeding alerts. Aborting.");
            return;
        }

        // 2) Precompute symbol pool
        List<String> symbols = new ArrayList<>(NSEStockSymbols.ALL_SYMBOLS);

        // 3) Generate alerts
        List<UserStockAlert> buffer = new ArrayList<>(BATCH_SIZE);
        int created = 0;

        // Distribute fairly: cycle through users until we reach TARGET_ALERTS
        outer:
        while (created < TARGET_ALERTS) {
            for (String userId : userIds) {
                // Pick 5–10 random symbols for this user
                List<String> chosenSymbols = pickRandomSymbols(symbols, 5 + rnd.nextInt(6));

                for (String symbol : chosenSymbols) {
                    int alertsForSymbol = 1 + rnd.nextInt(3); // 1–3 alerts per symbol
                    for (int i = 0; i < alertsForSymbol; i++) {
                        if (created >= TARGET_ALERTS) break outer;

                        UserStockAlert alert = new UserStockAlert();
                        alert.setActive(true);
                        alert.setUserId(userId);
                        alert.setSymbol(symbol);

                        alert.setThreshold(generateUniqueThreshold(userId, symbol, 
                        		SymbolPriceRanges.RANGES.getOrDefault(symbol, new SymbolPriceRanges.Range(200, 4000))));

                        // Random trigger type
                        alert.setTriggerType(rnd.nextBoolean() ? TriggerType.ABOVE : TriggerType.BELOW);
                        
                        String alertName = symbol + " " + alert.getTriggerType() + " Alert " + Double.valueOf(alert.getThreshold()).longValue();
                        alert.setAlertName(alertName);

                        // Random channels (1–3 unique)
                        alert.setChannels(randomChannels());

                        buffer.add(alert);
                        created++;

                        if (buffer.size() >= BATCH_SIZE) {
                            alertRepository.saveAll(buffer);
                            buffer.clear();
                            log.info("Seeded alerts so far: " + created);
                        }
                    }
                }
            }
        }

        if (!buffer.isEmpty()) {
            alertRepository.saveAll(buffer);
            buffer.clear();
        }
        usedThresholds.clear();
        log.info("Completed seeding {} UserStockAlert records.", created);
    }
    
    private double generateUniqueThreshold(String userId, String symbol, SymbolPriceRanges.Range range) {
        String key = userId + "|" + symbol;
        usedThresholds.putIfAbsent(key, new HashSet<>());

        double threshold;
        int attempts = 0;
        do {
            // Step 1: Generate base integer part within range
            int base = (int) ThreadLocalRandom.current().nextDouble(range.min, range.max);

            // Step 2: Pick a random fractional part from .05 to .95
            int fractionStep = ThreadLocalRandom.current().nextInt(1, 20); // 1 to 19
            double fraction = fractionStep * 0.05;

            threshold = base + fraction;
            threshold = Math.round(threshold * 100.0) / 100.0; // ensure two decimal places

            attempts++;
        } while (usedThresholds.get(key).contains(threshold) && attempts < 100);

        usedThresholds.get(key).add(threshold);
        return threshold;
    }

    private List<String> fetchUserIds(int limit) {
        return userRepository.findAll().stream()
                .map(u -> u.getUserId()) // adjust getter
                .filter(Objects::nonNull)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<String> pickRandomSymbols(List<String> symbols, int count) {
        List<String> copy = new ArrayList<>(symbols);
        Collections.shuffle(copy, rnd);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    private Set<NotificationChannel> randomChannels() {
        List<NotificationChannel> all = Arrays.asList(NotificationChannel.values());
        Collections.shuffle(all, rnd);
        int count = 1 + rnd.nextInt(Math.min(3, all.size())); // 1–3 channels
        return new HashSet<>(all.subList(0, count));
    }

    private double randomDouble(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
}
