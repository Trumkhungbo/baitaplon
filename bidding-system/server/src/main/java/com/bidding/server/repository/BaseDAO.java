package com.bidding.server.repository;

import com.bidding.server.database.DatabaseManager;
import java.sql.Connection;

public abstract class BaseDAO {

    protected Connection getConn(){
        return DatabaseManager
                .getInstance()
                .getConnection();
    }
}