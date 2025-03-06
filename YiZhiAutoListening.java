package com.YiZhi4Listening.main;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;
import com.google.gson.Gson;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author NSRobot
 * @version 1.0
 */
public class YiZhiAutoListening implements NativeKeyListener, NativeMouseInputListener, NativeMouseWheelListener {
    private StringBuilder keyRecord = new StringBuilder();
    private static int mouseWheelRecord = 0;
    private static boolean isRecord = false;
    private int unit4Wheel = 1;
    private Instant lastTime = Instant.now();
    private static ArrayList<Event> events = new ArrayList();
    private final AtomicInteger activeThreads = new AtomicInteger(0);

    public YiZhiAutoListening() {
    }

    public void nativeKeyPressed(NativeKeyEvent e) {
        String tmp = NativeKeyEvent.getKeyText(e.getKeyCode());
        if (this.keyRecord.isEmpty()) {
            this.keyRecord.append(tmp);
        } else {
            this.keyRecord.append("*" + tmp);
        }

        System.out.println("Key Pressed: " + tmp);
    }

    public void nativeKeyReleased(NativeKeyEvent e) {
        if (mouseWheelRecord != 0) {
            this.recordEvents4MouseWheelMove();
        }

        if (!this.keyRecord.isEmpty()) {
            this.recordDifferTime();
            System.out.println("Key Released,Get Combine Key: " + this.keyRecord.toString());
            this.recordEvents('G', this.keyRecord.toString());
        }

        if (this.keyRecord.toString().equals("Ctrl*Shift*Q")) {
            try {
                this.saveAsJson();
                this.writeEventsToExcel("src/main/resources");
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException var3) {
                var3.printStackTrace();
            }
        }

        this.keyRecord.setLength(0);
    }

    public void nativeMousePressed(NativeMouseEvent e) {
    }

    public void nativeMouseClicked(NativeMouseEvent e) {
        System.out.println("Mouse Clicked: " + e.getClickCount());
    }

    public void nativeMouseReleased(NativeMouseEvent e) {
        if (mouseWheelRecord != 0) {
            this.recordEvents4MouseWheelMove();
        }

        this.recordDifferTime();
        PrintStream var10000 = System.out;
        int var10001 = e.getButton();
        var10000.println("Mouse Released: " + var10001 + e.getX() + ", " + e.getY());
        int button = e.getButton();
        int var10002;
        switch (button) {
            case 1:
                var10002 = e.getX();
                this.recordEvents('C', "" + var10002 + "*" + e.getY());
                break;
            case 2:
                var10002 = e.getX();
                this.recordEvents('A', "" + var10002 + "*" + e.getY());
                break;
            case 3:
                var10002 = e.getX();
                this.recordEvents('M', "" + var10002 + "*" + e.getY());
        }

    }

    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        if (e.getWheelRotation() == 1) {
            mouseWheelRecord += this.unit4Wheel;
        } else if (e.getWheelRotation() == -1) {
            mouseWheelRecord -= this.unit4Wheel;
        }

    }

    public void recordEvents4MouseWheelMove() {
        this.recordDifferTime();
        System.out.println("Mosue Wheel Moved: " + mouseWheelRecord);
        this.recordEvents('B', "" + mouseWheelRecord);
        mouseWheelRecord = 0;
    }

    public static void main(String[] args) {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException var2) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(var2.getMessage());
            System.exit(1);
        }

        GlobalScreen.addNativeKeyListener(new YiZhiAutoListening());
        GlobalScreen.addNativeMouseListener(new YiZhiAutoListening());
        GlobalScreen.addNativeMouseMotionListener(new YiZhiAutoListening());
        GlobalScreen.addNativeMouseWheelListener(new YiZhiAutoListening());
    }

    public void saveAsJson() {
        Gson gson = new Gson();
        String json = gson.toJson(events);
        System.out.println("======================\n" + json);
    }

    private void recordEvents(char type, String detail) {
        Event event = new Event(type, detail);
        events.add(event);
    }

    private void recordDifferTime() {
        char F_type = 'F';
        Instant now = Instant.now();
        Duration duration = Duration.between(now, this.lastTime);
        long seconds = duration.getSeconds() * -1L;
        this.recordEvents(F_type, "" + seconds);
        this.lastTime = now;
    }

    private void writeEventsToExcel(String outputPath) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Events");
        int rowNum = 1;
        Iterator var5 = events.iterator();

        while(var5.hasNext()) {
            Event event = (Event)var5.next();
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("" + event.getType());
            row.createCell(1).setCellValue(event.getDetail());
        }

        for(int i = 0; i < 3; ++i) {
            sheet.autoSizeColumn(i);
        }

        try {
            FileOutputStream fileOut = new FileOutputStream(outputPath + "/events.xlsx");

            try {
                workbook.write(fileOut);
            } catch (Throwable var9) {
                try {
                    fileOut.close();
                } catch (Throwable var8) {
                    var9.addSuppressed(var8);
                }

                throw var9;
            }

            fileOut.close();
        } catch (Exception var10) {
            var10.printStackTrace();
        }

    }

    private class Event {
        private char type;
        private String detail;

        public Event(char type, String detail) {
            this.type = type;
            this.detail = detail;
        }

        public char getType() {
            return type;
        }

        public String getDetail() {
            return detail;
        }
    }

    private class EventType {
        public static final char MOUSE_RIGHT_CLICK = 'A';
        public static final char MOUSE_LEFT_CLICK = 'C';
        public static final char MOUSE_MID_CLICK = 'M';
        public static final char MOUSE_WHEEL_MOVE = 'B';
        public static final char MOUSE_MOVE = 'H';
        public static final char INPUT_CHARACTER = 'E';
        public static final char INPUT_KEYBOARD = 'G';
        public static final char TIME_DIFF = 'F';
    }
}