package com.bidding.server.database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseManager {

    private static DatabaseManager instance;

    private Connection connection;

    private DatabaseManager(){

        try{

            Class.forName(
              "org.sqlite.JDBC"
            );

            connection=
             DriverManager.getConnection(
               "jdbc:sqlite:data/auction.db"
             );

        }catch(Exception e){
            throw new RuntimeException(e);
        }

    }


    public static synchronized
    DatabaseManager getInstance(){

        if(instance==null){
            instance=
             new DatabaseManager();
        }

        return instance;
    }


    public Connection getConnection(){

        return connection;

    }

}