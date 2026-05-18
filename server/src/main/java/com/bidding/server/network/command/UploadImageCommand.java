package com.bidding.server.network.command;

import com.bidding.server.network.ClientHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

public class UploadImageCommand implements CommandHandler {

    // Thư mục lưu ảnh trên server
    private static final String IMAGE_DIR = "data/images/";

    public UploadImageCommand() {
        // Tạo thư mục nếu chưa có
        new File(IMAGE_DIR).mkdirs();
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        // Format: UPLOAD_IMAGE|extension|base64data
        // Ví dụ: UPLOAD_IMAGE|png|iVBORw0KGgoAAAANSUhEUgAA...
        if (parts.length < 3) {
            client.sendMessage("ERROR|Invalid syntax. Use: UPLOAD_IMAGE|extension|base64data");
            return;
        }

        String extension = parts[1].toLowerCase().replaceAll("[^a-z0-9]", "");
        String base64Data = parts[2];

        // Chỉ cho phép các định dạng ảnh hợp lệ
        if (!extension.matches("png|jpg|jpeg|gif|webp|jfif")) {
            client.sendMessage("ERROR|Unsupported image format: " + extension);
            return;
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // Tạo tên file unique
            String filename = UUID.randomUUID().toString() + "." + extension;
            File outputFile = new File(IMAGE_DIR + filename);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(imageBytes);
            }

            System.out.println("[IMAGE] Saved: " + filename + " (" + imageBytes.length + " bytes)");
            client.sendMessage("UPLOAD_IMAGE_SUCCESS|" + filename);

        } catch (IllegalArgumentException e) {
            client.sendMessage("ERROR|Invalid Base64 image data");
        } catch (IOException e) {
            client.sendMessage("ERROR|Failed to save image: " + e.getMessage());
        }
    }
}