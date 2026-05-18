package com.bidding.server.network.command;

import com.bidding.server.network.ClientHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class GetImageCommand implements CommandHandler {

    private static final String IMAGE_DIR = "data/images/";

    @Override
    public void handle(String[] parts, ClientHandler client) {
        // Format: GET_IMAGE|filename
        if (parts.length < 2) {
            client.sendMessage("ERROR|Invalid syntax. Use: GET_IMAGE|filename");
            return;
        }

        String filename = parts[1];

        // Chặn path traversal (ví dụ: "../secret.txt")
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            client.sendMessage("ERROR|Invalid filename");
            return;
        }

        File imageFile = new File(IMAGE_DIR + filename);

        if (!imageFile.exists() || !imageFile.isFile()) {
            client.sendMessage("IMAGE_NOT_FOUND|" + filename);
            return;
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);

            // Lấy extension để client biết loại ảnh
            String extension = filename.contains(".")
                    ? filename.substring(filename.lastIndexOf('.') + 1)
                    : "jpg";

            client.sendMessage("IMAGE_DATA|" + filename + "|" + extension + "|" + base64Data);

        } catch (IOException e) {
            client.sendMessage("ERROR|Failed to read image: " + e.getMessage());
        }
    }
}