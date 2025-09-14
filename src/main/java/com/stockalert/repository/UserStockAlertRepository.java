package com.stockalert.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stockalert.model.UserStockAlert;

import jakarta.persistence.LockModeType;

public interface UserStockAlertRepository extends JpaRepository<UserStockAlert, Long> {
	
	List<UserStockAlert> findByUserId(String userId);
    List<UserStockAlert> findBySymbol(String symbol);
    //@Query("SELECT a FROM UserStockAlert a WHERE a.symbol IN :symbols")
    List<UserStockAlert> findBySymbolIn(@Param("symbols") Collection<String> symbols);

    @Query("SELECT a FROM UserStockAlert a WHERE a.isActive = true AND a.symbol = :symbol AND a.threshold BETWEEN :min AND :max")
    List<UserStockAlert> findBySymbolAndThresholdRange(@Param("symbol") String symbol,
                                                       @Param("min") double min,
                                                       @Param("max") double max);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UserStockAlert a where a.id = :id")
    Optional<UserStockAlert> findByIdForUpdate(@Param("id") Long id);
    
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserStockAlert a
        set a.isTriggered = true,
            a.isActive = false
        where a.id = :id and a.isActive = true and a.isTriggered = false
        """)
    int markAsTriggeredOnce(@Param("id") Long id);
}