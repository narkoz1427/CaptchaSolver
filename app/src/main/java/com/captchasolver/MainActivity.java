package com.captchasolver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private EditText etApiKey;
    private TextView tvStatus;
    private TextView tvLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etApiKey = findViewById(R.id.etApiKey);
        tvStatus = findViewById(R.id.tvStatus);
        tvLog = findViewById(R.id.tvLog);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnAccessibility = findViewById(R.id.btnAccessibility);
        Button btnScreenCapture = findViewById(R.id.btnScreenCapture);

        SharedPreferences prefs = getSharedPreferences("captcha", Context.MODE_PRIVATE);
        String savedKey = prefs.getString("api_key", "");
        etApiKey.setText(savedKey);

        btnSave.setOnClickListener(v -> {
            String apiKey = etApiKey.getText().toString().trim();
            if (apiKey.isEmpty()) {
                Toast.makeText(this, "API Key boş olamaz", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString("api_key", apiKey).apply();
            GeminiHelper.setApiKey(apiKey);
            Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show();
            tvLog.setText("API Key kaydedildi.");
        });

        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnScreenCapture.setOnClickListener(v -> {
            MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        });

        if (!savedKey.isEmpty()) {
            GeminiHelper.setApiKey(savedKey);
        }

        updateStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == RESULT_OK) {
            Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
            serviceIntent.putExtra("resultCode", resultCode);
            serviceIntent.putExtra("data", data);
            startForegroundService(serviceIntent);
            tvLog.setText("Ekran yakalama izni verildi. Servis başlatıldı.");
            Toast.makeText(this, "Hazır! Oyuna geçebilirsiniz.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus() {
        SharedPreferences prefs = getSharedPreferences("captcha", Context.MODE_PRIVATE);
        String key = prefs.getString("api_key", "");
        tvStatus.setText("Durum: API Key " + (key.isEmpty() ? "girilmedi" : "mevcut"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
