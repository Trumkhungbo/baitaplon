package com.bidding.common.model.item;

import com.bidding.common.enums.ItemType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemModelTest {

    @Test
    void artConstructorsAndDetailsShouldUseArtFields() {
        Art emptyArt = new Art();
        assertEquals(ItemType.ART, emptyArt.getItemType());

        Art art = new Art("Mona Lisa", 10_000, "art.jpg", "Da Vinci", 1503);

        assertEquals("Mona Lisa", art.getName());
        assertEquals(10_000, art.getStartingPrice());
        assertEquals("art.jpg", art.getImageUrl());
        assertEquals("Da Vinci", art.getArtist());
        assertEquals(1503, art.getCreationYear());
        assertEquals(ItemType.ART, art.getItemType());
        assertTrue(art.getItemDetails().contains("artist='Da Vinci'"));
        assertEquals(art.getItemDetails(), art.toString());
    }

    @Test
    void artSettersShouldValidateRequiredFields() {
        Art art = new Art();

        assertThrows(IllegalArgumentException.class, () -> art.setArtist(""));
        assertThrows(IllegalArgumentException.class, () -> art.setCreationYear(0));

        art.setArtist("Picasso");
        art.setCreationYear(1937);

        assertEquals("Picasso", art.getArtist());
        assertEquals(1937, art.getCreationYear());
    }

    @Test
    void vehicleConstructorsAndDetailsShouldUseVehicleFields() {
        Vehicle emptyVehicle = new Vehicle();
        assertEquals(ItemType.VEHICLE, emptyVehicle.getItemType());

        Vehicle vehicle = new Vehicle("Toyota Camry", 20_000, "car.jpg", "Gasoline", 15_000);

        assertEquals("Toyota Camry", vehicle.getName());
        assertEquals(20_000, vehicle.getStartingPrice());
        assertEquals("car.jpg", vehicle.getImageUrl());
        assertEquals("Gasoline", vehicle.getEngineType());
        assertEquals(15_000, vehicle.getMileage());
        assertEquals(ItemType.VEHICLE, vehicle.getItemType());
        assertTrue(vehicle.getItemDetails().contains("mileage=15000 km"));
        assertEquals(vehicle.getItemDetails(), vehicle.toString());
    }

    @Test
    void vehicleSettersShouldValidateRequiredFields() {
        Vehicle vehicle = new Vehicle();

        assertThrows(IllegalArgumentException.class, () -> vehicle.setEngineType(" "));
        assertThrows(IllegalArgumentException.class, () -> vehicle.setMileage(-1));

        vehicle.setEngineType("Electric");
        vehicle.setMileage(0);

        assertEquals("Electric", vehicle.getEngineType());
        assertEquals(0, vehicle.getMileage());
    }

    @Test
    void electronicsConstructorsAndDetailsShouldUseElectronicsFields() {
        Electronics emptyElectronics = new Electronics();
        assertEquals(ItemType.ELECTRONICS, emptyElectronics.getItemType());

        Electronics electronics = new Electronics("Phone", 500, "phone.png", "Apple", 12);

        assertEquals("Phone", electronics.getName());
        assertEquals(500, electronics.getStartingPrice());
        assertEquals("phone.png", electronics.getImageUrl());
        assertEquals("Apple", electronics.getBrand());
        assertEquals(12, electronics.getWarrantyMonths());
        assertEquals(ItemType.ELECTRONICS, electronics.getItemType());
        assertTrue(electronics.getItemDetails().contains("brand='Apple'"));
        assertEquals(electronics.getItemDetails(), electronics.toString());
    }

    @Test
    void baseItemSettersShouldNormalizeNullableFields() {
        Electronics item = new Electronics();

        item.setName("Camera");
        item.setDescription(null);
        item.setImageUrl(null);
        item.setItemType(ItemType.OTHER);

        assertEquals("Camera", item.getName());
        assertEquals("", item.getDescription());
        assertEquals("", item.getImageUrl());
        assertEquals(ItemType.OTHER, item.getItemType());
        assertTrue(item.toString().contains("Camera"));
    }
}
