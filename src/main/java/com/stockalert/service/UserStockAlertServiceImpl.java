package com.stockalert.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.stockalert.common.AlertChangeEvent;
import com.stockalert.common.AlertChangeType;
import com.stockalert.common.UserStockAlertDTO;
import com.stockalert.kafka.producer.AlertChangeProducer;
import com.stockalert.model.UserStockAlert;
import com.stockalert.repository.UserStockAlertRepository;
import com.stockalert.util.UserStockAlertMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class UserStockAlertServiceImpl implements UserStockAlertService {

    @Autowired
    private UserStockAlertRepository repository;

    @Autowired
    private AlertChangeProducer producer;

    @Override
    public UserStockAlert createAlert(UserStockAlert alert) {
        UserStockAlert saved = repository.save(alert);
        publishAfterCommit(buildEvent(saved, AlertChangeType.CREATE));
        return saved;
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
            alert.setActive(updatedAlert.isActive());
            alert.setTriggered(false);
            alert.setChannels(updatedAlert.getChannels());
            UserStockAlert saved = repository.save(alert);
            publishAfterCommit(buildEvent(saved, AlertChangeType.UPDATE));
            return saved;
        }).orElse(null);
    }

    @Override
    public void deleteAlert(Long alertId) {
        repository.findById(alertId).ifPresent(existing -> {
            repository.deleteById(alertId);
            publishAfterCommit(new AlertChangeEvent(
                existing.getSymbol(), AlertChangeType.DELETE, existing.getAlertId(), null, System.currentTimeMillis()
            ));
        });
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
            publishAfterCommit(buildEvent(a, AlertChangeType.UPDATE));
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

    private AlertChangeEvent buildEvent(UserStockAlert entity, AlertChangeType type) {
        UserStockAlertDTO dto = UserStockAlertMapper.toAlertDTO(entity);
        return new AlertChangeEvent(entity.getSymbol(), type, entity.getAlertId(), dto, System.currentTimeMillis());
    }

    private void publishAfterCommit(AlertChangeEvent evt) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    producer.publish(evt);
                }
            });
        } else {
            producer.publish(evt);
        }
    }
}
