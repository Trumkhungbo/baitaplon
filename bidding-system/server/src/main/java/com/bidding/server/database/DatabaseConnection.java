package com.bidding.server.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseConnection {

    private static Connection connection;

    private DatabaseConnection(){}

    public static synchronized Connection getConnection(){

        try{

            if(connection==null ||
               connection.isClosed()){

                new File(
                   DatabaseConfig.DB_DIR
                ).mkdirs();

                Class.forName(
                  "org.sqlite.JDBC"
                );

                connection=
                   DriverManager.getConnection(
                    DatabaseConfig.DB_URL
                   );

                connection.setAutoCommit(true);

                try(
                 Statement st=
                  connection.createStatement()
                ){
                    st.execute(
                     "PRAGMA journal_mode=WAL"
                    );

                    st.execute(
                     "PRAGMA foreign_keys=ON"
                    );
                }
            }

        }catch(Exception e){
            throw new RuntimeException(e);
        }

        return connection;
    }

    public static void close(){

        try{

            if(connection!=null){
                connection.close();
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}