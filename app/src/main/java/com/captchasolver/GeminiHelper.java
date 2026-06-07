package com.captchasolver;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiHelper {

    private static final String TAG = "GeminiHelper";
    private static String apiKey = "";

    public static void setApiKey(String key) {
        apiKey = key;
    }

    public static int[] analyzeCaptcha(Bitmap bitmap) {
        try {
            // Bitmap'i base64'e çevir
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            // Gemini API isteği oluştur
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            // Resim parçası
            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", base64Image);
            imagePart.put("inline_data", inlineData);

            // Metin parçası
            JSONObject textPart = new JSONObject();
            textPart.put("text",
                "Bu ekran görüntüsünde 'Bot Dogrulama' başlıklı bir captcha var. " +
                "3x3 grid içinde 9 resim parçası var. " +
                "'Yanlis parcayi sec' yazıyor yani bütünü bozan, diğerlerine uymayan parçayı seçmem gerekiyor. " +
                "Hangi parça diğerleriyle uyumsuz veya bütünü bozuyor? " +
                "Sadece şu formatta cevap ver: ROW,COL " +
                "Satır ve sütun 1'den başlar, sol üst 1,1 - sağ alt 3,3 " +
                "Başka hiçbir şey yazma, sadece ROW,COL yaz.");

            parts.put(imagePart);
            parts.put(textPart);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            // HTTP isteği gönder
            String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            OutputStream os = conn.getOutputStream();
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == 200) {
                byte[] responseBytes = conn.getInputStream().readAllBytes();
                String responseStr = new String(responseBytes, StandardCharsets.UTF_8);
                Log.d(TAG, "Response: " + responseStr);

                // Cevabı parse et
                JSONObject response = new JSONObject(responseStr);
                String text = response
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim();

                Log.d(TAG, "Gemini cevabı: " + text);

                // ROW,COL parse et
                String[] parts2 = text.split(",");
                if (parts2.length == 2) {
                    int row = Integer.parseInt(parts2[0].trim());
                    int col = Integer.parseInt(parts2[1].trim());
                    return new int[]{row, col};
                }
            } else {
                byte[] errBytes = conn.getErrorStream().readAllBytes();
                Log.e(TAG, "Hata: " + new String(errBytes, StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            Log.e(TAG, "Hata: " + e.getMessage(), e);
        }
        return null;
    }
}
