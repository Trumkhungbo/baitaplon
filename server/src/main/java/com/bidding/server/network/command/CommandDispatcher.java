package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

import java.util.HashMap;
import java.util.Map;

public class CommandDispatcher {

    private final Map<String, CommandHandler> handlers = new HashMap<>();

    public CommandDispatcher(AuthService authService,
                             AuctionService auctionService,
                             BroadcastService broadcastService) {
        register("PING", new PingCommand());
        register("REGISTER", new RegisterCommand(authService));
        register("LOGIN", new LoginCommand(authService));
        register("LIST_AUCTIONS", new ListAuctionsCommand(auctionService));
        register("WATCH", new WatchAuctionCommand());
        register("BID", new BidCommand(auctionService, broadcastService));
        register("QUIT", new QuitCommand());
        register("GET_AUCTION_DETAIL", new GetAuctionDetailCommand(auctionService));
        register("GET_BID_HISTORY", new GetBidHistoryCommand(auctionService));
        register("ADD_AUCTION", new AddAuctionCommand(auctionService, broadcastService));
        register("APPROVE_AUCTION", new ApproveAuctionCommand(auctionService, broadcastService));
        register("SET_AUTO_BID", new SetAutoBidCommand(auctionService, broadcastService));
        register("CLOSE_AUCTION", new CloseAuctionCommand(auctionService, broadcastService));
        register("GET_WINNER", new GetWinnerCommand(auctionService));
        register("GET_ACCOUNTINFORMATION", new GetAccountInformationCommand(authService));
        register("ADD_MONEY", new GetNewMoneyCommand(authService));
        register("FORGOT_PASSWORD", new ForgotPasswordCommand(authService));
        register("RESET_PASSWORD", new ResetPasswordCommand(authService));
        register("UPDATE_STATUS", new UpdateStatusCommand(auctionService));
        register("ELEVATE", new ElevateCommand());
        register("UPLOAD_IMAGE", new UploadImageCommand());
        register("GET_IMAGE", new GetImageCommand());
    }

    public void dispatch(String command, String[] parts, ClientHandler client) {
        CommandHandler handler = handlers.get(command);

        if (handler == null) {
            client.sendMessage("ERROR|Invalid command: " + command);
            return;
        }

        handler.handle(parts, client);
    }

    private void register(String command, CommandHandler handler) {
        handlers.put(command, handler);
    }
}