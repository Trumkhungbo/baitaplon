package action.SellingJobs;

import action.Authentication.StoreDataInput;
import action.Authentication.StoreItemDataInit;
import action.MainUI.LobbyHandle;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class InvesmentWaitHandle implements Initializable, SocketListener {

    @FXML private VBox productListBox;
    @FXML private Label totalCountLabel;
    @FXML private Label runningCountLabel;
    @FXML private Label upcomingCountLabel;
    @FXML private Label finishedCountLabel;
    @FXML private Label feedbackLabel;
    @FXML private TextField searchField;
    @FXML private ChoiceBox<String> typeFilter;
    @FXML private ChoiceBox<String> statusFilter;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");
    private final List<MyProductRowData> products = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SocketClient.getInstance().addListener(this);
        setupFilters();
        loadProducts();
    }

    private void setupFilters() {
        typeFilter.getItems().setAll("Tất cả danh mục", "Điện tử", "Nghệ thuật", "Xe cộ");
        typeFilter.setValue("Tất cả danh mục");
        statusFilter.getItems().setAll("Tất cả trạng thái", "Đang đấu giá", "Sắp diễn ra", "Chờ duyệt", "Đã kết thúc");
        statusFilter.setValue("Tất cả trạng thái");
    }

    @FXML
    public void refreshProducts() {
        loadProducts();
    }

    @FXML
    public void applyFilters() {
        renderProducts();
    }

    @FXML
    public void goToAddProduct() throws IOException {
        StoreSellerProductEdit.clear();
        if (LobbyHandle.getInstance() != null) {
            LobbyHandle.getInstance().MovingCenter("/views/InvesterSell.fxml");
        }
    }

    private void loadProducts() {
        JsonObject req = new JsonObject();
        req.addProperty("command", "LIST_MY_AUCTIONS");
        req.addProperty("sellerUsername", StoreDataInput.getUsername());
        SocketClient.getInstance().requestData(req.toString());
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            if (data == null || data.isBlank()) {
                return;
            }

            if (data.startsWith("MY_AUCTIONS|")) {
                parseMyAuctions(data.substring("MY_AUCTIONS|".length()));
                renderProducts();
                return;
            }

            if (data.startsWith("DELETE_AUCTION_SUCCESS|") || data.startsWith("UPDATE_AUCTION_SUCCESS|")) {
                setFeedback(data.startsWith("DELETE_AUCTION_SUCCESS|")
                        ? "Xóa sản phẩm thành công."
                        : "Cập nhật sản phẩm thành công.");
                loadProducts();
                return;
            }

            if (data.startsWith("ERROR|")) {
                setFeedback(data.substring("ERROR|".length()));
            }
        });
    }

    private void parseMyAuctions(String payload) {
        products.clear();
        if (payload.isBlank()) {
            return;
        }

        for (String row : payload.split(";")) {
            String[] attr = row.split(":", -1);
            if (attr.length < 17) {
                continue;
            }

            products.add(new MyProductRowData(
                    attr[0],
                    attr[1],
                    attr[2],
                    attr[3],
                    parseDouble(attr[4]),
                    parseDouble(attr[5]),
                    attr[6],
                    attr[7],
                    attr[8],
                    attr[9],
                    parseInt(attr[10]),
                    attr[11],
                    attr[12],
                    attr[13],
                    attr[14],
                    attr[15],
                    attr[16]
            ));
        }

        products.sort(Comparator.comparingInt((MyProductRowData item) -> parseInt(item.auctionId())).reversed());
    }

    private void renderProducts() {
        productListBox.getChildren().clear();
        updateSummary();

        List<MyProductRowData> filtered = products.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesType)
                .filter(this::matchesStatus)
                .toList();

        for (MyProductRowData product : filtered) {
            productListBox.getChildren().add(createRow(product));
        }

        if (filtered.isEmpty()) {
            Label empty = new Label("Không có sản phẩm phù hợp bộ lọc hiện tại.");
            empty.setStyle("-fx-text-fill: #8B8B92; -fx-font-size: 14px; -fx-padding: 30 10 10 10;");
            productListBox.getChildren().add(empty);
        }
    }

    private boolean matchesSearch(MyProductRowData item) {
        String keyword = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (keyword.isBlank()) {
            return true;
        }
        return item.itemName().toLowerCase().contains(keyword)
                || item.auctionId().toLowerCase().contains(keyword);
    }

    private boolean matchesType(MyProductRowData item) {
        if (typeFilter == null || typeFilter.getValue() == null || "Tất cả danh mục".equals(typeFilter.getValue())) {
            return true;
        }
        return normalizeType(item.itemType()).equals(typeFilter.getValue());
    }

    private boolean matchesStatus(MyProductRowData item) {
        if (statusFilter == null || statusFilter.getValue() == null || "Tất cả trạng thái".equals(statusFilter.getValue())) {
            return true;
        }
        return switch (statusFilter.getValue()) {
            case "Đang đấu giá" -> "RUNNING".equalsIgnoreCase(item.status());
            case "Sắp diễn ra" -> "OPEN".equalsIgnoreCase(item.status());
            case "Chờ duyệt" -> "PENDING".equalsIgnoreCase(item.status());
            case "Đã kết thúc" -> "FINISHED".equalsIgnoreCase(item.status());
            default -> true;
        };
    }

    private void updateSummary() {
        totalCountLabel.setText(String.valueOf(products.size()));
        runningCountLabel.setText(String.valueOf(products.stream().filter(p -> "RUNNING".equalsIgnoreCase(p.status())).count()));
        upcomingCountLabel.setText(String.valueOf(products.stream().filter(p -> "OPEN".equalsIgnoreCase(p.status()) || "PENDING".equalsIgnoreCase(p.status())).count()));
        finishedCountLabel.setText(String.valueOf(products.stream().filter(p -> "FINISHED".equalsIgnoreCase(p.status())).count()));
    }

    private HBox createRow(MyProductRowData product) {
        HBox row = new HBox(10.0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12.0, 10.0, 12.0, 10.0));
        row.getStyleClass().add("my-product-row");
        row.setOnMouseClicked(event -> openAuctionRoom(product));

        HBox productCell = new HBox(12.0);
        productCell.setPrefWidth(270.0);
        productCell.setAlignment(Pos.CENTER_LEFT);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(56.0);
        imageView.setFitHeight(56.0);
        imageView.setPreserveRatio(true);
        imageView.setMouseTransparent(true);
        imageView.setStyle("-fx-background-color: #1A1A1A; -fx-background-radius: 10;");

        if (product.imageUrl() != null && !product.imageUrl().isBlank()) {
            loadImage(product.imageUrl(), imageView);
        }

        VBox productMeta = new VBox(4.0);
        Label productName = new Label(product.itemName());
        productName.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        Label productCode = new Label("#AU" + product.auctionId());
        productCode.setStyle("-fx-text-fill: #777777; -fx-font-size: 11px;");
        productMeta.getChildren().addAll(productName, productCode);
        productCell.getChildren().addAll(imageView, productMeta);

        Label typeLabel = createPlainCell(normalizeType(product.itemType()), 120.0, "#D4D4D8");
        Label startPrice = createPlainCell(formatMoney(product.startPrice()) + " VNĐ", 140.0, "#F3F4F6");
        Label currentPrice = createPlainCell(formatMoney(product.currentPrice()) + " VNĐ", 140.0, "#4ADE80");
        Label statusBadge = createStatusBadge(product.status());
        statusBadge.setPrefWidth(120.0);

        VBox timeBox = new VBox(4.0);
        timeBox.setPrefWidth(160.0);
        Label timeMain = new Label(product.startDate() + " " + product.startClockTime());
        timeMain.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        Label timeSub = new Label(product.durationMinutes() + " phút • " + product.bidCount() + " lượt bid");
        timeSub.setStyle("-fx-text-fill: #8B8B92; -fx-font-size: 11px;");
        timeBox.getChildren().addAll(timeMain, timeSub);

        HBox actions = new HBox(8.0);
        actions.setPrefWidth(180.0);

        Button editBtn = new Button("Sửa");
        editBtn.getStyleClass().add("outline-gold-button");
        editBtn.setOnAction(event -> {
            event.consume();
            openEdit(product);
        });

        Button deleteBtn = new Button("Xóa");
        deleteBtn.getStyleClass().add("outline-danger-button");
        deleteBtn.setOnAction(event -> {
            event.consume();
            deleteProduct(product);
        });
        actions.getChildren().addAll(editBtn, deleteBtn);

        row.getChildren().addAll(productCell, typeLabel, startPrice, currentPrice, statusBadge, timeBox, actions);
        return row;
    }

    private void openEdit(MyProductRowData product) {
        if (isLockedByStatus(product)) {
            setFeedback("Bạn không thể sửa vì phiên đấu giá đang diễn ra hoặc đã kết thúc.");
            return;
        }

        StoreSellerProductEdit.editing = true;
        StoreSellerProductEdit.auctionId = product.auctionId();
        StoreSellerProductEdit.itemName = product.itemName();
        StoreSellerProductEdit.itemType = product.itemType();
        StoreSellerProductEdit.description = product.description();
        StoreSellerProductEdit.information1 = product.information1();
        StoreSellerProductEdit.information2 = product.information2();
        StoreSellerProductEdit.price = String.valueOf((long) product.startPrice());
        StoreSellerProductEdit.date = product.startDate();
        StoreSellerProductEdit.time = product.startClockTime();
        StoreSellerProductEdit.duration = product.durationMinutes();
        StoreSellerProductEdit.imageUrl = product.imageUrl();

        try {
            if (LobbyHandle.getInstance() != null) {
                LobbyHandle.getInstance().MovingCenter("/views/InvesterSell.fxml");
            }
        } catch (IOException e) {
            setFeedback("Không thể mở form sửa sản phẩm.");
        }
    }

    private void openAuctionRoom(MyProductRowData product) {
        StoreItemDataInit.name = product.itemName();
        StoreItemDataInit.description = product.auctionId();
        StoreItemDataInit.price = formatMoney(product.currentPrice());
        StoreItemDataInit.status = product.status();
        StoreItemDataInit.image = product.imageUrl();
        StoreItemDataInit.itemInformation1 = product.information1();
        StoreItemDataInit.itemInformation2 = product.information2();
        StoreItemDataInit.itemType = product.itemType();

        try {
            if (LobbyHandle.getInstance() != null) {
                LobbyHandle.getInstance().ItemShowing();
            }
        } catch (IOException e) {
            setFeedback("Không thể mở phòng đấu giá của sản phẩm.");
        }
    }

    private void deleteProduct(MyProductRowData product) {
        if (isLockedByStatus(product)) {
            setFeedback("Bạn không thể xóa vì phiên đấu giá đang diễn ra hoặc đã kết thúc.");
            return;
        }

        JsonObject req = new JsonObject();
        req.addProperty("command", "DELETE_AUCTION");
        req.addProperty("auctionId", product.auctionId());
        SocketClient.getInstance().requestData(req.toString());
    }

    private boolean isLockedByStatus(MyProductRowData product) {
        return "RUNNING".equalsIgnoreCase(product.status())
                || "FINISHED".equalsIgnoreCase(product.status());
    }

    private void loadImage(String filename, ImageView imageView) {
        SocketClient.getInstance().requestData("GET_IMAGE|" + filename);
        SocketClient.getInstance().addListener(new SocketListener() {
            @Override
            public void onDataReceived(String data) {
                if (data == null || !data.startsWith("IMAGE_DATA|")) {
                    return;
                }
                String[] parts = data.split("\\|", 4);
                if (parts.length < 4 || !filename.equals(parts[1])) {
                    return;
                }
                Platform.runLater(() -> {
                    try {
                        byte[] bytes = Base64.getDecoder().decode(parts[3]);
                        imageView.setImage(new Image(new ByteArrayInputStream(bytes)));
                    } catch (IllegalArgumentException ignored) {
                    }
                });
                SocketClient.getInstance().removeListener(this);
            }
        });
    }

    private Label createPlainCell(String text, double width, String color) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
        return label;
    }

    private Label createStatusBadge(String status) {
        String style;
        String text;
        switch (status.toUpperCase()) {
            case "RUNNING" -> {
                style = "-fx-background-color: rgba(34,197,94,0.16); -fx-text-fill: #4ADE80; -fx-background-radius: 999; -fx-padding: 6 10; -fx-font-weight: bold;";
                text = "ĐANG ĐẤU GIÁ";
            }
            case "OPEN" -> {
                style = "-fx-background-color: rgba(59,130,246,0.16); -fx-text-fill: #60A5FA; -fx-background-radius: 999; -fx-padding: 6 10; -fx-font-weight: bold;";
                text = "SẮP DIỄN RA";
            }
            case "PENDING" -> {
                style = "-fx-background-color: rgba(250,204,21,0.16); -fx-text-fill: #FACC15; -fx-background-radius: 999; -fx-padding: 6 10; -fx-font-weight: bold;";
                text = "CHỜ DUYỆT";
            }
            default -> {
                style = "-fx-background-color: rgba(148,163,184,0.16); -fx-text-fill: #CBD5E1; -fx-background-radius: 999; -fx-padding: 6 10; -fx-font-weight: bold;";
                text = "ĐÃ KẾT THÚC";
            }
        }

        Label label = new Label(text);
        label.setStyle(style);
        return label;
    }

    private String formatMoney(double value) {
        return moneyFormat.format(value);
    }

    private void setFeedback(String message) {
        if (feedbackLabel != null) {
            feedbackLabel.setText(message == null ? "" : message);
        }
    }

    private String normalizeType(String type) {
        return switch (type.toUpperCase()) {
            case "ELECTRONICS" -> "Điện tử";
            case "ART" -> "Nghệ thuật";
            case "VEHICLE" -> "Xe cộ";
            default -> type;
        };
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private record MyProductRowData(
            String auctionId,
            String itemId,
            String itemName,
            String itemType,
            double startPrice,
            double currentPrice,
            String status,
            String startDate,
            String startClockTime,
            String durationMinutes,
            int bidCount,
            String imageUrl,
            String description,
            String information1,
            String information2,
            String startTimeMillis,
            String endTimeMillis
    ) {
    }
}
