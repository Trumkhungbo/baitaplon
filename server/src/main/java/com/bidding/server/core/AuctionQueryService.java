package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.item.Item;
import com.bidding.server.repository.AuctionRecordDAO;
import com.bidding.server.repository.AuctionStateDAO;
import com.bidding.server.repository.BidHistoryDAO;
import com.bidding.server.repository.ItemDAO;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý các thao tác đọc auction và format dữ liệu trả về client.
 */
class AuctionQueryService {

    private final Map<String, Auction> auctions;
    private final BidHistoryDAO bidHistoryDAO;
    private final AuctionStateDAO auctionStateDAO;
    private final AuctionRecordDAO auctionRecordDAO;
    private final ItemDAO itemDAO;
    private final AuctionResponseFormatter formatter = new AuctionResponseFormatter();

    AuctionQueryService(
            Map<String, Auction> auctions,
            BidHistoryDAO bidHistoryDAO,
            AuctionStateDAO auctionStateDAO,
            AuctionRecordDAO auctionRecordDAO,
            ItemDAO itemDAO
    ) {
        this.auctions = auctions;
        this.bidHistoryDAO = bidHistoryDAO;
        this.auctionStateDAO = auctionStateDAO;
        this.auctionRecordDAO = auctionRecordDAO;
        this.itemDAO = itemDAO;
    }

    /**
     * Trả các dòng auction cho lobby, có thể bao gồm auction PENDING.
     */
    String getAuctionList(boolean includePending) {
        StringBuilder sb = new StringBuilder("AUCTION_LIST|");
        boolean first = true;

        for (Auction auction : auctions.values()) {
            if (!shouldIncludeInAuctionList(auction, includePending)) {
                continue;
            }

            if (!first) {
                sb.append(";");
            }

            appendAuctionListRow(sb, auction);
            first = false;
        }

        return sb.toString();
    }

    /**
     * Tìm một auction và đồng bộ trạng thái runtime trước khi trả về.
     */
    Auction findAuctionById(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return null;
        }

        syncAuctionFromDatabase(auction);
        applyTimeBasedStatus(auction, System.currentTimeMillis());

        return auction;
    }

    /**
     * Trả danh sách auction thuộc một seller.
     */
    String getSellerAuctionList(String sellerUsername) {
        if (sellerUsername == null || sellerUsername.isBlank()) {
            return "ERROR|Seller username required";
        }

        StringBuilder sb = new StringBuilder("MY_AUCTIONS|");
        boolean first = true;

        for (Auction auction : auctions.values().stream()
                .filter(a -> sellerUsername.equals(a.getSellerUsername()))
                .sorted((left, right) -> Integer.compare(
                        Integer.parseInt(right.getId()),
                        Integer.parseInt(left.getId())))
                .toList()) {

            syncAuctionFromDatabase(auction);
            applyTimeBasedStatus(auction, System.currentTimeMillis());

            Item item = itemDAO.findById(auction.getItemId());
            if (item == null) {
                continue;
            }

            if (!first) {
                sb.append(";");
            }
            first = false;

            appendAccountAuctionRow(sb, auction, item, "Seller", auction.getStartDate(), auction.getStartClockTime(), "", false);
        }

        return sb.toString();
    }

    /**
     * Trả danh sách auction mà tài khoản đã từng bid.
     */
    String getAccountAuctionList(String username) {
        if (username == null || username.isBlank()) {
            return "ERROR|Username required";
        }

        StringBuilder sb = new StringBuilder("ACCOUNT_AUCTIONS|");
        boolean first = true;

        Map<String, Long> bidderAuctionTimes = bidHistoryDAO.findLatestBidTimesByBidder(username);
        for (Map.Entry<String, Long> entry : bidderAuctionTimes.entrySet()) {
            Auction auction = auctions.get(entry.getKey());
            if (auction == null || username.equals(auction.getSellerUsername())) {
                continue;
            }
            Item item = prepareAuctionForAccountList(auction);
            if (item == null) {
                continue;
            }
            if (!first) {
                sb.append(";");
            }
            first = false;
            String result = resolveBidderResultText(auction, username);
            boolean canPay = "FINISHED".equalsIgnoreCase(auction.getStatus().name())
                    && username.equalsIgnoreCase(auction.getHighestBidder());
            appendAccountAuctionRow(sb, auction, item, "Bidder", formatter.formatEpochDate(entry.getValue()), formatter.formatEpochTime(entry.getValue()), result, canPay);
        }

        return sb.toString();
    }

    /**
     * Trả response chi tiết sản phẩm cho một auction.
     */
    String getAuctionDetail(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        syncAuctionFromDatabase(auction);
        applyTimeBasedStatus(auction, System.currentTimeMillis());

        String bidder = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();
        String[] itemFields = resolveAuctionDetailItemFields(auction);
        String statusStr = auction.getStatus() == null ? "UNKNOWN" : auction.getStatus().name();

        return "AUCTION_DETAIL"
                + "|id=" + auction.getId()
                + "|seller=" + auction.getSellerUsername()
                + "|itemName=" + auction.getItemName()
                + "|startPrice=" + (long) auction.getStartPrice()
                + "|currentPrice=" + (long) auction.getCurrentPrice()
                + "|highestBidder=" + bidder
                + "|status=" + statusStr
                + "|startDate=" + auction.getStartDate()
                + "|startTime=" + auction.getStartClockTime()
                + "|duration=" + auction.getDurationMinutes()
                + "|bidCount=" + bidHistoryDAO.countByAuctionId(auctionId)
                + "|imageUrl=" + itemFields[0]
                + "|information1=" + itemFields[1]
                + "|information2=" + itemFields[2]
                + "|description=" + formatter.sanitizeMessageValue(getItemDescription(auction.getItemId()))
                + "|itemType=" + itemFields[3]
                + "|endTime=" + auction.getEndTime()
                + "|serverTime=" + System.currentTimeMillis();
    }

    /**
     * Trả thông tin sản phẩm rút gọn cho request cũ.
     */
    String getProductInfo(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        syncAuctionFromDatabase(auction);

        String bidder = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return "PRODUCT_INFO"
                + "|id=" + auction.getId()
                + "|seller=" + auction.getSellerUsername()
                + "|itemName=" + auction.getItemName()
                + "|startPrice=" + (long) auction.getStartPrice()
                + "|currentPrice=" + (long) auction.getCurrentPrice()
                + "|highestBidder=" + bidder
                + "|status=" + auction.getStatus()
                + "|bidCount="
                + bidHistoryDAO.countByAuctionId(auctionId);
    }

    /**
     * Trả còác dng lịch sử bid của một auction.
     */
    String getBidHistory(String auctionId) {
        if (!auctions.containsKey(auctionId)) {
            return "ERROR|Auction not found";
        }

        StringBuilder sb = new StringBuilder("BID_HISTORY|auctionId=" + auctionId + "|entries=");
        boolean first = true;

        for (BidRecord record : bidHistoryDAO.findByAuctionId(auctionId)) {
            if (!first) {
                sb.append(";");
            }

            sb.append(record.getBidderUsername())
                    .append(",")
                    .append((long) record.getAmount())
                    .append(",")
                    .append(record.getTimestamp());

            first = false;
        }

        return sb.toString();
    }

    private boolean shouldIncludeInAuctionList(Auction auction, boolean includePending) {
        return includePending
                || auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING
                || auction.getStatus() == AuctionStatus.FINISHED;
    }

    private void appendAuctionListRow(StringBuilder sb, Auction auction) {
        String id = auction.getId() == null ? "" : auction.getId();
        String itemName = formatter.sanitizeListValue(auction.getItemName() == null ? "Unknown Item" : auction.getItemName());
        String status = auction.getStatus() == null ? "UNKNOWN" : auction.getStatus().name();
        String imageUrl = "";
        String description = "";
        String information1 = "";
        String information2 = "";
        String itemType = "";
        String startDate = formatter.sanitizeListValue(auction.getStartDate());
        String startTime = formatter.sanitizeListValue(auction.getStartClockTime());
        String duration = String.valueOf(auction.getDurationMinutes());
        try {
            Item item = itemDAO.findById(auction.getItemId());
            if (item != null) {
                if (item.getItemType() != null) itemType = formatter.sanitizeListValue(item.getItemType().name());
                if (item.getImageUrl() != null) imageUrl = formatter.sanitizeListValue(item.getImageUrl());
                if (item.getDescription() != null) description = formatter.sanitizeListValue(item.getDescription());
                String resolvedInformation1 = itemDAO.resolveInformation1(item);
                String resolvedInformation2 = itemDAO.resolveInformation2(item);
                if (resolvedInformation1 != null) information1 = formatter.sanitizeListValue(resolvedInformation1);
                if (resolvedInformation2 != null) information2 = formatter.sanitizeListValue(resolvedInformation2);
            }
        } catch (RuntimeException e) {
            System.err.println("[AUCTION_LIST] Cannot load image for auction " + id + ": " + e.getMessage());
        }

        sb.append(id).append(":")
                .append(itemName).append(":")
                .append((long) auction.getCurrentPrice()).append(":")
                .append(status).append(":")
                .append(imageUrl).append(":")
                .append(description).append(":")
                .append(information1).append(":")
                .append(information2).append(":")
                .append(startDate).append(":")
                .append(startTime).append(":")
                .append(duration).append(":")
                .append(itemType).append(":")
                .append(auction.getEndTime()).append(":")
                .append(System.currentTimeMillis());
    }

    private Item prepareAuctionForAccountList(Auction auction) {
        syncAuctionFromDatabase(auction);
        applyTimeBasedStatus(auction, System.currentTimeMillis());
        return itemDAO.findById(auction.getItemId());
    }

    private void appendAccountAuctionRow(StringBuilder sb, Auction auction, Item item, String role, String displayDate, String displayTime, String result, boolean canPay) {
        String itemType = item.getItemType() == null ? "" : item.getItemType().name();
        String imageUrl = formatter.sanitizeListValue(item.getImageUrl());
        String description = formatter.sanitizeListValue(item.getDescription());
        String information1 = formatter.sanitizeListValue(itemDAO.resolveInformation1(item));
        String information2 = formatter.sanitizeListValue(itemDAO.resolveInformation2(item));
        int bidCount = bidHistoryDAO.countByAuctionId(auction.getId());

        sb.append(auction.getId()).append(":")
                .append(auction.getItemId()).append(":")
                .append(formatter.sanitizeListValue(auction.getItemName())).append(":")
                .append(itemType).append(":")
                .append((long) auction.getStartPrice()).append(":")
                .append((long) auction.getCurrentPrice()).append(":")
                .append(auction.getStatus().name()).append(":")
                .append(formatter.sanitizeListValue(displayDate)).append(":")
                .append(formatter.sanitizeListValue(displayTime)).append(":")
                .append(auction.getDurationMinutes()).append(":")
                .append(bidCount).append(":")
                .append(imageUrl).append(":")
                .append(description).append(":")
                .append(information1).append(":")
                .append(information2).append(":")
                .append(auction.getStartTimeMillis()).append(":")
                .append(auction.getEndTime()).append(":")
                .append(role).append(":")
                .append(formatter.sanitizeListValue(result)).append(":")
                .append(canPay);
    }

    private String resolveBidderResultText(Auction auction, String username) {
        if (auction == null || username == null || username.isBlank()) return "";
        String status = auction.getStatus() == null ? "" : auction.getStatus().name();
        if ("RUNNING".equalsIgnoreCase(status)) {
            int rank = calculateBidderRank(auction.getId(), username);
            return rank > 0 ? "Top " + rank : "";
        }
        if ("FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            return username.equalsIgnoreCase(auction.getHighestBidder()) ? "winner" : "lost";
        }
        if ("CANCELED".equalsIgnoreCase(status)) return "canceled";
        return "";
    }

    private int calculateBidderRank(String auctionId, String username) {
        Map<String, BidRecord> ranking = buildBidderRanking(auctionId);
        int rank = 1;
        for (BidRecord record : ranking.values().stream()
                .sorted((left, right) -> {
                    int amountCompare = Double.compare(right.getAmount(), left.getAmount());
                    if (amountCompare != 0) return amountCompare;
                    return Long.compare(left.getTimestamp(), right.getTimestamp());
                })
                .toList()) {
            if (username.equalsIgnoreCase(record.getBidderUsername())) return rank;
            rank++;
        }
        return 0;
    }

    private Map<String, BidRecord> buildBidderRanking(String auctionId) {
        Map<String, BidRecord> bestBids = new HashMap<>();
        for (BidRecord record : bidHistoryDAO.findByAuctionId(auctionId)) {
            BidRecord current = bestBids.get(record.getBidderUsername());
            if (current == null
                    || record.getAmount() > current.getAmount()
                    || (record.getAmount() == current.getAmount() && record.getTimestamp() < current.getTimestamp())) {
                bestBids.put(record.getBidderUsername(), record);
            }
        }
        return bestBids;
    }

    private String[] resolveAuctionDetailItemFields(Auction auction) {
        String imageUrl = "";
        String information1 = "";
        String information2 = "";
        String itemType = "";

        try {
            Item item = itemDAO.findById(auction.getItemId());
            if (item != null) {
                if (item.getImageUrl() != null) imageUrl = item.getImageUrl();
                if (item.getItemType() != null) itemType = item.getItemType().name();
                String description = item.getDescription();
                String resolvedInformation1 = itemDAO.resolveInformation1(item);
                String resolvedInformation2 = itemDAO.resolveInformation2(item);
                if (resolvedInformation1 != null) information1 = resolvedInformation1;
                if (resolvedInformation2 != null) information2 = resolvedInformation2;
                if (description != null) information2 = information2 == null ? "" : information2;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[]{imageUrl, information1, information2, itemType};
    }

    private String getItemDescription(long itemId) {
        try {
            Item item = itemDAO.findById(itemId);
            return item == null ? "" : item.getDescription();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private boolean applyTimeBasedStatus(Auction auction, long now) {
        if (auction == null) return false;
        if (auction.getStatus() == AuctionStatus.OPEN
                && now >= auction.getStartTimeMillis()
                && now < auction.getEndTime()) {
            auction.setStatus(AuctionStatus.RUNNING);
            persistAuctionState(auction);
            return true;
        }
        return false;
    }

    private AuctionStateDAO.AuctionStateSnapshot syncAuctionFromDatabase(Auction auction) {
        AuctionStateDAO.AuctionStateSnapshot state = auctionStateDAO.findByAuctionId(auction.getId());
        if (state == null) return null;
        auction.setCurrentPrice(state.currentPrice());
        auction.setStatus(state.status());
        auction.setHighestBidder(state.highestBidder());
        auction.setStartTimeMillis(state.startTimeMillis());
        auction.setDurationMinutes(state.durationMinutes());
        if (state.endTimeMillis() > auction.getEndTime()) auction.setEndTime(state.endTimeMillis());
        return state;
    }

    private void persistAuctionState(Auction auction) {
        auctionStateDAO.upsert(auction, bidHistoryDAO.countByAuctionId(auction.getId()));
        auctionRecordDAO.updateState(auction);
    }
}
