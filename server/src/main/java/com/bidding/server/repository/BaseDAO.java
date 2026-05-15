package com.bidding.server.repository;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.database.DatabaseManager;
import java.sql.Connection;

public abstract class BaseDAO {
    protected Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    protected AuctionStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return AuctionStatus.PENDING;
        }
        try {
            return AuctionStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AuctionStatus.PENDING;
        }
    }
}
