package com.stockalert.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stockalert.model.UserStockAlert;
import com.stockalert.repository.UserStockAlertRepository;

@Service
@Transactional
public class UserStockAlertServiceImpl implements UserStockAlertService {

	@Autowired
	private UserStockAlertRepository repository;

	@Override
	public UserStockAlert createAlert(UserStockAlert alert) {
		return repository.save(alert);
	}

	@Override
	public UserStockAlert getAlertById(Long alertId) {
		return repository.findById(alertId).orElse(null);
	}

	@Override
	public List<UserStockAlert> getAllAlerts() {
		return repository.findAll();
	}

	@Override
	public UserStockAlert updateAlert(Long alertId, UserStockAlert updatedAlert) {
		return repository.findById(alertId).map(alert -> {
			alert.setAlertName(updatedAlert.getAlertName());
			alert.setUserId(updatedAlert.getUserId());
			alert.setSymbol(updatedAlert.getSymbol());
			alert.setThreshold(updatedAlert.getThreshold());
			alert.setTriggerType(updatedAlert.getTriggerType());
			alert.setChannels(updatedAlert.getChannels());
			return repository.save(alert);
		}).orElse(null);
	}

	@Override
	public void deleteAlert(Long alertId) {
		repository.deleteById(alertId);
	}

	@Override
	public List<UserStockAlert> getAlertsByUserId(String userId) {
		return repository.findByUserId(userId);
	}

	@Override
	public List<UserStockAlert> getAlertsBySymbol(String symbol) {
		return repository.findBySymbol(symbol);
	}

	@Override
	public void markAsTriggered(Long id) {
		repository.markAsTriggeredOnce(id);
	}

	@Override
	public boolean reactivate(Long id) {
		return repository.findByIdForUpdate(id).map(a -> {
			a.setActive(true);
			a.setTriggered(false);
			return true;
		}).orElse(false);
	}

	@Override
	public List<UserStockAlert> getAlertsBySymbolAndThresholdRange(String symbol, double min, double max) {
		return repository.findBySymbolAndThresholdRange(symbol, min, max);
	}

	@Override
	public List<UserStockAlert> getAlertsBySymbols(List<String> list) {
		return repository.findBySymbolIn(list);
	}
}