package com.bidding.server.database;

import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize(){

        String users="""
        CREATE TABLE IF NOT EXISTS users(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT UNIQUE,
            password_hash TEXT,
            email TEXT UNIQUE,
            created_at INTEGER
        )
        """;

        String auctions="""
        CREATE TABLE IF NOT EXISTS auctions(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            item_id INTEGER,
            seller_id INTEGER,
            current_price REAL
        )
        """;

        try(
         Statement st=
           DatabaseConnection
            .getConnection()
            .createStatement()
        ){

            st.execute(users);
            st.execute(auctions);

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}