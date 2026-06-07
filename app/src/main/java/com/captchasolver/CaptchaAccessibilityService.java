package com.captchasolver;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptchaAccessibilityService extends AccessibilityService {

    private static final String TAG = "CaptchaService";
    private static CaptchaAccessibilityService instance;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isProcessing = false;

    private static final int GRID_LEFT = 57;
    private static final int GRID_TOP = 280;
    private static final int GRID_RIGHT = 423;
    private static final int GRID_BOTTOM = 580;

    public static CaptchaAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Accessibility Service bağlandı");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isProcessing) return;

        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;

            if (isCaptchaVisible(root)) {
                if (!isProcessing) {
                    isProcessing = true;
                    Log.d(TAG, "Captcha tespit edildi!");
                    executor.execute(this::solveCaptcha);
                }
            }
            root.recycle();
        }
    }

    private boolean isCaptchaVisible(AccessibilityNodeInfo root) {
        return findNodeWithText(root, "Bot Dogrulama") != null ||
                findNodeWithText(root, "Yanlis parcayi") != null;
    }

    private AccessibilityNodeInfo findNodeWithText(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().contains(text)) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findNodeWithText(child, text);
            if (result != null) return result;
            if (child != null) child.recycle();
        }
        return null;
    }

    private void solveCaptcha() {
        try {
            Thread.sleep(1000);

            ScreenCaptureService captureService = ScreenCaptureService.getInstance();
            if (captureService == null) {
                Log.e(TAG, "ScreenCaptureService hazır değil!");
                isProcessing = false;
                return;
            }

            Bitmap screen = captureService.captureScreen();
            if (screen == null) {
                Log.e(TAG, "Ekran görüntüsü alınamadı!");
                isProcessing = false;
                return;
            }

            Log.d(TAG, "Ekran görüntüsü alındı, Gemini'ye gönderiliyor...");

            int[] result = GeminiHelper.analyzeCaptcha(screen);
            screen.recycle();

            if (result == null) {
                Log.e(TAG, "Gemini cevap vermedi!");
                isProcessing = false;
                return;
            }

            int row = result[0];
            int col = result[1];

            Log.d(TAG, "Tıklanacak hücre: satır=" + row + " sütun=" + col);

            int cellWidth = (GRID_RIGHT - GRID_LEFT) / 3;
            int cellHeight = (GRID_BOTTOM - GRID_TOP) / 3;
            int x = GRID_LEFT + (col - 1) * cellWidth + cellWidth / 2;
            int y = GRID_TOP + (row - 1) * cellHeight + cellHeight / 2;

            Log.d(TAG, "Tıklanacak koordinat: x=" + x + " y=" + y);

            Thread.sleep(500);
            performTap(x, y);
            Log.d(TAG, "Tıklandı!");
            Thread.sleep(2000);

        } catch (Exception e) {
            Log.e(TAG, "Hata: " + e.getMessage(), e);
        } finally {
            isProcessing = false;
        }
    }

    private void performTap(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
        dispatchGesture(builder.build(), null, null);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Servis kesildi");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        executor.shutdown();
    }
}
