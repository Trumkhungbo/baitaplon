package com.bidding.server.database;

public class DatabaseManager {

    private static DatabaseManager instance;

    private DatabaseManager(){
        DatabaseInitializer.initialize();
    }

    public static synchronized
    DatabaseManager getInstance(){

        if(instance==null){
            instance=
              new DatabaseManager();
        }

        return instance;
    }

    public void shutdown(){
        DatabaseConnection.close();
    }
}