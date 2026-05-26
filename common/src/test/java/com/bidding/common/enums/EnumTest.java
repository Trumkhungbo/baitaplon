package com.bidding.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumTest {

    @Test
    void auctionStatusShouldExposeExpectedValues() {
        assertArrayEquals(
                new AuctionStatus[]{
                        AuctionStatus.PENDING,
                        AuctionStatus.OPEN,
                        AuctionStatus.RUNNING,
                        AuctionStatus.FINISHED,
                        AuctionStatus.PAID,
                        AuctionStatus.CANCELED
                },
                AuctionStatus.values()
        );
        assertEquals(AuctionStatus.RUNNING, AuctionStatus.valueOf("RUNNING"));
    }

    @Test
    void itemTypeShouldExposeExpectedValues() {
        assertArrayEquals(
                new ItemType[]{
                        ItemType.ELECTRONICS,
                        ItemType.ART,
                        ItemType.VEHICLE,
                        ItemType.OTHER
                },
                ItemType.values()
        );
        assertEquals(ItemType.OTHER, ItemType.valueOf("OTHER"));
    }

    @Test
    void userRoleShouldExposeExpectedValues() {
        assertArrayEquals(
                new UserRole[]{
                        UserRole.BIDDER,
                        UserRole.SELLER,
                        UserRole.ADMIN
                },
                UserRole.values()
        );
        assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
    }

    @Test
    void depositStatusShouldExposeExpectedValues() {
        assertArrayEquals(
                new DepositStatus[]{
                        DepositStatus.PENDING,
                        DepositStatus.APPROVED,
                        DepositStatus.REJECTED
                },
                DepositStatus.values()
        );
        assertEquals(DepositStatus.APPROVED, DepositStatus.valueOf("APPROVED"));
    }
}
