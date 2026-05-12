package com.bidding.server;

import action.Core.StartScence;
import com.bidding.server.database.DatabaseInitializer;
import com.bidding.server.network.AuctionServer;
import javafx.application.Application;

public class ServerApplication {
    public static void main(String[] args) {
        try {
            System.out.println("[SYSTEM] Đang kiểm tra và khởi tạo Database...");
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            System.err.println("[CRITICAL] Không thể khởi tạo Database. Server sẽ dừng.");
            e.printStackTrace();
            return; // Nếu DB lỗi thì không cho chạy Server nữa
        }

        Thread serverThread = new Thread(()->{
        int port = 888;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port. Using default port 888");
            }
        }

        AuctionServer server = new AuctionServer(port);
        server.start();
    });
    serverThread.setDaemon(true);
    serverThread.start();
    // ở đây Deamon như kiểu mấy cái thread khác đóng sẽ tự đóng cái này
    Application.launch(StartScence.class, args);

}}
