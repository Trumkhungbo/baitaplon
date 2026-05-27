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
        register("LIST_MY_AUCTIONS", new ListMyAuctionsCommand(auctionService));
        register("LIST_ACCOUNT_AUCTIONS", new ListAccountAuctionsCommand(auctionService));
        register("GET_TRANSACTIONS", new GetTransactionsCommand(authService));
        register("ADD_AUCTION", new AddAuctionCommand(auctionService, broadcastService));
        register("UPDATE_AUCTION", new UpdateAuctionCommand(auctionService, broadcastService));
        register("APPROVE_AUCTION", new ApproveAuctionCommand(auctionService, broadcastService));
        register("DELETE_AUCTION", new DeleteAuctionCommand(auctionService, broadcastService));
        register("SET_AUTO_BID", new SetAutoBidCommand(auctionService, broadcastService));
        register("GET_AUTO_BID", new GetAutoBidCommand(auctionService));
        register("DISABLE_AUTO_BID", new DisableAutoBidCommand(auctionService, broadcastService));
        register("CLOSE_AUCTION", new CloseAuctionCommand(auctionService, broadcastService));
        register("GET_WINNER", new GetWinnerCommand(auctionService));
        register("PAY_AUCTION", new PayAuctionCommand(auctionService, broadcastService));
        register("GET_ACCOUNTINFORMATION", new GetAccountInformationCommand(authService));
        register("ADD_MONEY", new GetNewMoneyCommand(authService));
        register("ADMIN_LIST_USERS", new AdminListUsersCommand(authService));
        register("ADMIN_DELETE_USER", new AdminDeleteUserCommand(authService));
        register("ADMIN_LIST_TOPUP_REQUESTS", new AdminListTopUpRequestsCommand(authService));
        register("ADMIN_APPROVE_TOPUP_REQUEST", new AdminApproveTopUpRequestCommand(authService));
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
