package com.stockalert.util;

import com.stockalert.common.UserStockAlertDTO;
import com.stockalert.model.UserStockAlert;

public class UserStockAlertMapper {
	public static UserStockAlertDTO toAlertDTO(UserStockAlert alert) {
		return new UserStockAlertDTO(alert.getAlertId(), alert.getUserId(), alert.getSymbol(), alert.getThreshold(),
				alert.getTriggerType(), alert.isActive(), alert.isTriggered());
	}
}