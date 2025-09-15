package com.stockalert.util;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.stockalert.common.NSEStockSymbols;
import com.stockalert.common.NotificationChannel;
import com.stockalert.common.TriggerType;
import com.stockalert.model.UserStockAlert;
import com.stockalert.repository.UserRepository;
import com.stockalert.repository.UserStockAlertRepository;
import com.stockalert.service.UserStockAlertService;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserStockAlertSeeder implements CommandLineRunner {

    private static final int TARGET_ALERTS = 10_000;
    private static final int BATCH_SIZE = 1_000;

    // Continuous load settings
    private static final int OPS_PER_SECOND = 100;
    private static final long PERIOD_MS = 10000L / OPS_PER_SECOND;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserStockAlertRepository alertRepository;
    @Autowired
    private UserStockAlertService alertService;

    private Map<String, Set<Double>> usedThresholds = new HashMap<>();
    private final Random rnd = new SecureRandom();
    private List<String> userIds;
    private List<String> symbols;

    @Override
    @Transactional
    public void run(String... args) {
        // One-time bulk seed
        if (alertRepository.count() == 0) {
            bulkSeed();
        } else {
            log.info("UserStockAlert seeding skipped (alerts already present).");
        }

        // Preload for continuous ops
        userIds = fetchUserIds(300);
        symbols = new ArrayList<>(NSEStockSymbols.ALL_SYMBOLS);
        if (userIds.isEmpty()) {
            log.warn("No users found — continuous seeding will not run.");
        } else {
            log.info("Continuous seeding will run at {} ops/sec", OPS_PER_SECOND);
        }
    }

    private void bulkSeed() {
        List<String> users = fetchUserIds(300);
        if (users.isEmpty()) {
            log.info("No users found for seeding alerts. Aborting.");
            return;
        }
        List<String> allSymbols = new ArrayList<>(NSEStockSymbols.ALL_SYMBOLS);
        List<UserStockAlert> buffer = new ArrayList<>(BATCH_SIZE);
        int created = 0;

        outer:
        while (created < TARGET_ALERTS) {
            for (String userId : users) {
                List<String> chosenSymbols = pickRandomSymbols(allSymbols, 5 + rnd.nextInt(6));
                for (String symbol : chosenSymbols) {
                    int alertsForSymbol = 1 + rnd.nextInt(3);
                    for (int i = 0; i < alertsForSymbol; i++) {
                        if (created >= TARGET_ALERTS) break outer;
                        UserStockAlert alert = buildRandomAlert(userId, symbol);
                        buffer.add(alert);
                        created++;
                        if (buffer.size() >= BATCH_SIZE) {
                            alertRepository.saveAll(buffer);
                            buffer.clear();
                            log.info("Seeded alerts so far: {}", created);
                        }
                    }
                }
            }
        }
        if (!buffer.isEmpty()) {
            alertRepository.saveAll(buffer);
        }
        usedThresholds.clear();
        log.info("Completed seeding {} UserStockAlert records.", created);
    }

    private UserStockAlert buildRandomAlert(String userId, String symbol) {
        UserStockAlert alert = new UserStockAlert();
        alert.setActive(true);
        alert.setUserId(userId);
        alert.setSymbol(symbol);
        alert.setThreshold(generateUniqueThreshold(userId, symbol,
                SymbolPriceRanges.RANGES.getOrDefault(symbol, new SymbolPriceRanges.Range(200, 4000))));
        alert.setTriggerType(rnd.nextBoolean() ? TriggerType.ABOVE : TriggerType.BELOW);
        alert.setAlertName(symbol + " " + alert.getTriggerType() + " Alert " + (long) alert.getThreshold());
        alert.setChannels(randomChannels());
        return alert;
    }

    private double generateUniqueThreshold(String userId, String symbol, SymbolPriceRanges.Range range) {
        String key = userId + "|" + symbol;
        usedThresholds.putIfAbsent(key, new HashSet<>());
        double threshold;
        int attempts = 0;
        do {
            int base = (int) ThreadLocalRandom.current().nextDouble(range.min, range.max);
            int fractionStep = ThreadLocalRandom.current().nextInt(1, 20);
            double fraction = fractionStep * 0.05;
            threshold = Math.round((base + fraction) * 100.0) / 100.0;
            attempts++;
        } while (usedThresholds.get(key).contains(threshold) && attempts < 100);
        usedThresholds.get(key).add(threshold);
        return threshold;
    }

    private List<String> fetchUserIds(int limit) {
        return userRepository.findAll().stream()
                .map(u -> u.getUserId())
                .filter(Objects::nonNull)
                .distinct()
                .limit(limit)
                .toList();
    }

    private List<String> pickRandomSymbols(List<String> symbols, int count) {
        List<String> copy = new ArrayList<>(symbols);
        Collections.shuffle(copy, rnd);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    private Set<NotificationChannel> randomChannels() {
        List<NotificationChannel> all = Arrays.asList(NotificationChannel.values());
        Collections.shuffle(all, rnd);
        int count = 1 + rnd.nextInt(Math.min(3, all.size()));
        return new HashSet<>(all.subList(0, count));
    }

    // ---------------- Continuous Scheduled Ops ----------------

    /**
     * Runs continuously after startup, performing ~OPS_PER_SECOND random create/update ops.
     */
    @Scheduled(fixedRate = PERIOD_MS)
    public void continuousOps() {
        if (userIds == null || userIds.isEmpty()) return;

        try {
            if (rnd.nextBoolean()) {
                // Create
                String userId = randomUserId();
                String symbol = randomSymbol();
                UserStockAlert alert = buildRandomAlert(userId, symbol);
                alertService.createAlert(alert);
                log.info("Created alert for {} at {}", symbol, alert.getThreshold());
            } else {
                // Update
                List<UserStockAlert> all = alertRepository.findAll();
                if (all.isEmpty()) {
                    return;
                }
                UserStockAlert existing = all.get(rnd.nextInt(all.size()));
                existing.setThreshold(randomThreshold(existing.getSymbol()));
                existing.setTriggerType(rnd.nextBoolean() ? TriggerType.ABOVE : TriggerType.BELOW);
                existing.setActive(rnd.nextBoolean());
                existing.setTriggered(false);
                alertService.updateAlert(existing.getAlertId(), existing);
                log.info("Updated alert {}", existing.getAlertId());
            }
        } catch (Exception e) {
            log.error("Continuous seeder op failed", e);
        }
    }

    private String randomUserId() {
        return userIds.get(rnd.nextInt(userIds.size()));
    }

    private String randomSymbol() {
        return symbols.get(rnd.nextInt(symbols.size()));
    }

    private double randomThreshold(String symbol) {
        SymbolPriceRanges.Range range = SymbolPriceRanges.RANGES
                .getOrDefault(symbol, new SymbolPriceRanges.Range(200, 4000));
        double val = range.min + (range.max - range.min) * rnd.nextDouble();
        return Math.round(val * 100.0) / 100.0;
    }
}