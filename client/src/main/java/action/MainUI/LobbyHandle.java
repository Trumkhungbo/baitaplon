package action.MainUI;

import action.SocketClient;
import action.SocketListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LobbyHandle implements Initializable {

    private static final String VIEW_HOME = "/views/Lobbypane.fxml";
    private static final String VIEW_ABOUT = "/views/DesignerHouner.fxml";
    private static final String VIEW_SELLER = "/views/InvesterSell.fxml";
    private static final String VIEW_MY_PRODUCTS = "/views/InvesmentWait.fxml";
    private static final String VIEW_AUCTION = "/views/InvesmentSite.fxml";
    private static final String VIEW_AUCTION_DETAIL = "/views/ItemShowing.fxml";
    private static final String VIEW_ACCOUNT = "/views/AccountInformation.fxml";

    private static LobbyHandle instance;

    @FXML public BorderPane ShowingStage;
    @FXML private ToggleButton navHomeButton;
    @FXML private ToggleButton navAboutButton;
    @FXML private ToggleButton navSellerButton;
    @FXML private ToggleButton navMyProductsButton;
    @FXML private ToggleButton navAuctionButton;

    public LobbyHandle() {
        instance = this;
    }

    public static LobbyHandle getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            MovingCenter(VIEW_HOME);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void MovingCenter(String url) throws IOException {
        if (ShowingStage.getCenter() != null) {
            Object oldController = ShowingStage.getCenter().getUserData();
            if (oldController instanceof SocketListener listener) {
                SocketClient.getInstance().removeListener(listener);
            }
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(url));
        Parent root = loader.load();
        root.setUserData(loader.getController());
        ShowingStage.setCenter(root);
        updateNavbarSelection(url);
    }

    private void updateNavbarSelection(String url) {
        clearNavbarSelection();

        if (VIEW_HOME.equals(url)) {
            navHomeButton.setSelected(true);
        } else if (VIEW_ABOUT.equals(url)) {
            navAboutButton.setSelected(true);
        } else if (VIEW_SELLER.equals(url)) {
            navSellerButton.setSelected(true);
        } else if (VIEW_MY_PRODUCTS.equals(url)) {
            navMyProductsButton.setSelected(true);
        } else if (VIEW_AUCTION.equals(url) || VIEW_AUCTION_DETAIL.equals(url)) {
            navAuctionButton.setSelected(true);
        } else if (VIEW_ACCOUNT.equals(url)) {
            // giu nguyen trang thai navbar truoc do khi vao trang tai khoan
        }
    }

    private void clearNavbarSelection() {
        navHomeButton.setSelected(false);
        navAboutButton.setSelected(false);
        navSellerButton.setSelected(false);
        navMyProductsButton.setSelected(false);
        navAuctionButton.setSelected(false);
    }

    public void ReturnLobby(ActionEvent event) throws IOException {
        MovingCenter(VIEW_HOME);
    }

    public void ReturnInvesmentSite(ActionEvent event) throws IOException {
        MovingCenter(VIEW_AUCTION);
    }

    public void ReturnInvesmentWait(ActionEvent event) throws IOException {
        MovingCenter(VIEW_MY_PRODUCTS);
    }

    public void ReturnDesiner(ActionEvent event) throws IOException {
        MovingCenter(VIEW_ABOUT);
    }

    public void ReturnSeller(ActionEvent event) throws IOException {
        MovingCenter(VIEW_SELLER);
    }

    public void AccountInformation(ActionEvent event) throws IOException {
        MovingCenter(VIEW_ACCOUNT);
    }

    public void ItemShowing() throws IOException {
        MovingCenter(VIEW_AUCTION_DETAIL);
    }
}
