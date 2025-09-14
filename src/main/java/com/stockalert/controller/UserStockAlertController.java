package com.stockalert.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockalert.model.UserStockAlert;
import com.stockalert.service.UserStockAlertService;

@RestController
@RequestMapping("/api/alerts")
public class UserStockAlertController {

    @Autowired
    private UserStockAlertService service;

    @PostMapping
    public UserStockAlert createAlert(@RequestBody UserStockAlert alert) {
        return service.createAlert(alert);
    }

    @GetMapping("/{id}")
    public UserStockAlert getAlertById(@PathVariable("id") Long id) {
        return service.getAlertById(id);
    }

    @GetMapping
    public List<UserStockAlert> getAllAlerts() {
        return service.getAllAlerts();
    }

    @PutMapping("/{id}")
    public UserStockAlert updateAlert(@PathVariable("id") Long id, @RequestBody UserStockAlert alert) {
        return service.updateAlert(id, alert);
    }

    @DeleteMapping("/{id}")
    public void deleteAlert(@PathVariable("id") Long id) {
        service.deleteAlert(id);
    }

    @GetMapping("/user/{userId}")
    public List<UserStockAlert> getAlertsByUserId(@PathVariable("userId") String userId) {
        return service.getAlertsByUserId(userId);
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<UserStockAlert>> getAlertsBySymbol(@PathVariable("symbol") String symbol) {
        List<UserStockAlert> alerts = service.getAlertsBySymbol(symbol);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts for a symbol within a price threshold range
     * Example: GET /api/alerts/symbol/RELIANCE/range?min=2700&max=2800
     */
    @GetMapping("/symbol/{symbol}/range")
    public ResponseEntity<List<UserStockAlert>> getAlertsBySymbolAndRange(
            @PathVariable("symbol") String symbol,
            @RequestParam("min") double min,
            @RequestParam("max") double max) {
        List<UserStockAlert> alerts = service.getAlertsBySymbolAndThresholdRange(symbol, min, max);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alerts for multiple symbols
     * Example: GET /api/alerts/symbols?list=RELIANCE,TCS,INFY
     */
    @GetMapping("/symbols")
    public ResponseEntity<List<UserStockAlert>> getAlertsBySymbols(@RequestParam("symbols") List<String> list) {
        List<UserStockAlert> alerts = service.getAlertsBySymbols(list);
        return ResponseEntity.ok(alerts);
    }
    
    @PutMapping("/{id}/triggered")
    public ResponseEntity<Void> markAsTriggered(@PathVariable("id") Long id) {
    	this.service.markAsTriggered(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable("id") Long id) {
        boolean ok = service.reactivate(id);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}