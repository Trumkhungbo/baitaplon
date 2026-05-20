package action.SellingJobs;

public class StoreSellerProductEdit {
    public static boolean editing = false;
    public static String auctionId = "";
    public static String itemName = "";
    public static String itemType = "ELECTRONICS";
    public static String description = "";
    public static String information1 = "";
    public static String information2 = "";
    public static String price = "";
    public static String date = "";
    public static String time = "";
    public static String duration = "";
    public static String imageUrl = "";

    public static void clear() {
        editing = false;
        auctionId = "";
        itemName = "";
        itemType = "ELECTRONICS";
        description = "";
        information1 = "";
        information2 = "";
        price = "";
        date = "";
        time = "";
        duration = "";
        imageUrl = "";
    }
}
