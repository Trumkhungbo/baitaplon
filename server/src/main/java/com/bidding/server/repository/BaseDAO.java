package com.bidding.server.repository;

import com.bidding.server.database.DatabaseManager;
import java.sql.Connection;

/** Abstract base class cho tất cả DAO — cung cấp connection. */
public abstract class BaseDAO {
    protected Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }
}