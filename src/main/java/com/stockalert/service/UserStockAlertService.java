package com.stockalert.service;

import java.util.List;

import com.stockalert.model.UserStockAlert;

public interface UserStockAlertService {
    UserStockAlert createAlert(UserStockAlert alert);
    UserStockAlert getAlertById(Long alertId);
    List<UserStockAlert> getAllAlerts();
    UserStockAlert updateAlert(Long alertId, UserStockAlert updatedAlert);
    void deleteAlert(Long alertId);
    List<UserStockAlert> getAlertsByUserId(String userId);
    List<UserStockAlert> getAlertsBySymbol(String symbol);
    void markAsTriggered(Long alertId);
    boolean reactivate(Long id);
	List<UserStockAlert> getAlertsBySymbolAndThresholdRange(String symbol, double min, double max);
	List<UserStockAlert> getAlertsBySymbols(List<String> list);
}