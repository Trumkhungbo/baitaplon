package com.bidding.server.database;

public class DatabaseConfig {

    public static final String DB_DIR="data";
    public static final String DB_FILE=
            DB_DIR+"/auction.db";

    public static final String DB_URL=
            "jdbc:sqlite:"+DB_FILE;

    private DatabaseConfig(){}
}