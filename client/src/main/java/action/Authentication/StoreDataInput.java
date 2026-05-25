package action.Authentication;
public class StoreDataInput {
    static String password;
    static String username;

    private StoreDataInput() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String getUsername() {
        return username;
    }
}
