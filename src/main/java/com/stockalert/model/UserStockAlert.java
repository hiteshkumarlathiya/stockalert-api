package com.stockalert.model;

import java.util.Set;

import com.stockalert.common.NotificationChannel;
import com.stockalert.common.TriggerType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_stock_alerts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStockAlert {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long alertId;
	private String alertName;
    private String userId;
    private String symbol;
    private double threshold;
    private boolean isActive;       // controls if alert is eligible
    private boolean isTriggered;   // tracks if it has fired once


    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;
    
    @ElementCollection(targetClass = NotificationChannel.class)
    @CollectionTable(name = "user_stock_alert_channels", joinColumns = @JoinColumn(name = "alert_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private Set<NotificationChannel> channels;
}