package com.bidding.common.model.user;

import com.bidding.common.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserModelTest {

    @Test
    void adminConstructorsShouldAssignAdminRole() {
        Admin emptyAdmin = new Admin();
        Admin admin = new Admin("admin", "hash", "admin@example.com");

        assertEquals(UserRole.ADMIN, emptyAdmin.getRole());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals("admin", admin.getUsername());
        assertEquals("hash", admin.getPasswordHash());
        assertEquals("hash", admin.getPassword());
        assertEquals("admin@example.com", admin.getEmail());
    }

    @Test
    void sellerConstructorsShouldAssignSellerRole() {
        Seller emptySeller = new Seller();
        Seller seller = new Seller("seller", "hash", "seller@example.com");

        assertEquals(UserRole.SELLER, emptySeller.getRole());
        assertEquals(UserRole.SELLER, seller.getRole());
        assertEquals("seller", seller.getUsername());
        assertEquals("hash", seller.getPasswordHash());
        assertEquals("seller@example.com", seller.getEmail());
    }

    @Test
    void bidderConstructorShouldAssignBidderFields() {
        Bidder bidder = new Bidder("bidder", "hash", "bidder@example.com", 1_000, "0123", "PID1");

        assertEquals(UserRole.BIDDER, bidder.getRole());
        assertEquals("bidder", bidder.getUsername());
        assertEquals("hash", bidder.getPasswordHash());
        assertEquals("bidder@example.com", bidder.getEmail());
        assertEquals(1_000, bidder.getBalance());
        assertEquals("0123", bidder.getPhone());
        assertEquals("PID1", bidder.getPersonalId());
    }

    @Test
    void userSettersShouldUpdateSharedFields() {
        Bidder bidder = new Bidder();

        bidder.setUsername("new-user");
        bidder.setPasswordHash("hash1");
        bidder.setPassword("hash2");
        bidder.setEmail("new@example.com");
        bidder.setPhone("0987");
        bidder.setPersonalId("PID2");
        bidder.setRole(UserRole.SELLER);
        bidder.setBalance(2_000);

        assertEquals("new-user", bidder.getUsername());
        assertEquals("hash2", bidder.getPasswordHash());
        assertEquals("hash2", bidder.getPassword());
        assertEquals("new@example.com", bidder.getEmail());
        assertEquals("0987", bidder.getPhone());
        assertEquals("PID2", bidder.getPersonalId());
        assertEquals(UserRole.SELLER, bidder.getRole());
        assertEquals(2_000, bidder.getBalance());
    }
}
