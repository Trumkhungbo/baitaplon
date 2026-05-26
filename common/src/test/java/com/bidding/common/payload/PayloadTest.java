package com.bidding.common.payload;

import com.bidding.common.enums.ItemType;
import com.bidding.common.enums.UserRole;
import com.bidding.common.model.item.Electronics;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayloadTest {

    @Test
    void bidAndAuctionIdRequestsShouldStoreAuctionIds() {
        BidRequest bidRequest = new BidRequest();
        AuctionIdRequest auctionIdRequest = new AuctionIdRequest();
        DisableAutoBidRequest disableRequest = new DisableAutoBidRequest();

        bidRequest.setAuctionId(1);
        bidRequest.setBidAmount(1_500);
        auctionIdRequest.setAuctionId(2);
        disableRequest.setAuctionId(3);

        assertEquals(1, bidRequest.getAuctionId());
        assertEquals(1_500, bidRequest.getBidAmount());
        assertEquals(2, auctionIdRequest.getAuctionId());
        assertEquals(3, disableRequest.getAuctionId());
    }

    @Test
    void loginPayloadsShouldStoreCredentialsAndResponseData() {
        LoginRequest request = new LoginRequest();
        LoginResponse response = new LoginResponse();

        request.setUsername("user1");
        request.setPassword("secret");
        response.setToken("token");
        response.setUsername("user1");
        response.setRole(UserRole.BIDDER);
        response.setUserId(99);

        assertEquals("user1", request.getUsername());
        assertEquals("secret", request.getPassword());
        assertEquals("token", response.getToken());
        assertEquals("user1", response.getUsername());
        assertEquals(UserRole.BIDDER, response.getRole());
        assertEquals(99, response.getUserId());
    }

    @Test
    void registerRequestShouldStoreRegistrationData() {
        RegisterRequest request = new RegisterRequest();

        request.setUsername("new-user");
        request.setPassword("secret");
        request.setEmail("new@example.com");
        request.setFullName("New User");
        request.setRole(UserRole.SELLER);
        request.setInitialBalance(500);

        assertEquals("new-user", request.getUsername());
        assertEquals("secret", request.getPassword());
        assertEquals("new@example.com", request.getEmail());
        assertEquals("New User", request.getFullName());
        assertEquals(UserRole.SELLER, request.getRole());
        assertEquals(500, request.getInitialBalance());
    }

    @Test
    void createItemRequestShouldStoreAllItemTypeSpecificFields() {
        CreateItemRequest request = new CreateItemRequest();

        request.setName("Camera");
        request.setDescription("Mirrorless camera");
        request.setStartingPrice(700);
        request.setItemType(ItemType.ELECTRONICS);
        request.setArtist("Artist");
        request.setCreationYear(2020);
        request.setBrand("Sony");
        request.setWarrantyMonths(24);
        request.setEngineType("Electric");
        request.setMileage(100);

        assertEquals("Camera", request.getName());
        assertEquals("Mirrorless camera", request.getDescription());
        assertEquals(700, request.getStartingPrice());
        assertEquals(ItemType.ELECTRONICS, request.getItemType());
        assertEquals("Artist", request.getArtist());
        assertEquals(2020, request.getCreationYear());
        assertEquals("Sony", request.getBrand());
        assertEquals(24, request.getWarrantyMonths());
        assertEquals("Electric", request.getEngineType());
        assertEquals(100, request.getMileage());
    }

    @Test
    void createAuctionRequestShouldStoreAuctionCreationData() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        Electronics item = new Electronics("Phone", 500, "phone.png", "Apple", 12);
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 25, 10, 0);
        LocalDateTime endTime = startTime.plusHours(2);

        request.setType("CREATE_AUCTION");
        request.setSellerUsername("seller");
        request.setItem(item);
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        assertEquals("CREATE_AUCTION", request.getType());
        assertEquals("seller", request.getSellerUsername());
        assertEquals(item, request.getItem());
        assertEquals(startTime, request.getStartTime());
        assertEquals(endTime, request.getEndTime());
    }

    @Test
    void autoBidRequestShouldStoreAutoBidConfiguration() {
        RegisterAutoBidRequest request = new RegisterAutoBidRequest();

        request.setAuctionId(5);
        request.setMaxBid(10_000);
        request.setIncrement(500);

        assertEquals(5, request.getAuctionId());
        assertEquals(10_000, request.getMaxBid());
        assertEquals(500, request.getIncrement());
    }

    @Test
    void responseMsgShouldSupportConstructorAndSetters() {
        ResponseMsg<List<String>> response = new ResponseMsg<>(200, "OK", List.of("a", "b"));

        assertEquals(200, response.getStatus());
        assertEquals("OK", response.getMessage());
        assertEquals(List.of("a", "b"), response.getData());

        response.setStatus(400);
        response.setMessage("BAD_REQUEST");
        response.setData(List.of("error"));

        assertEquals(400, response.getStatus());
        assertEquals("BAD_REQUEST", response.getMessage());
        assertEquals(List.of("error"), response.getData());
    }
}
