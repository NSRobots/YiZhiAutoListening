package com.YiZhi4Listening.main;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author NSRobot
 * @version 1.0
 */
public class YiZhiAutoListening implements NativeKeyListener, NativeMouseInputListener, NativeMouseWheelListener {
    private StringBuilder keyRecord = new StringBuilder(); // 组合键判断容器
    private ConcurrentLinkedDeque<String> st4CombineKey = new ConcurrentLinkedDeque<>(); // 按键记录，解决”ctrl*A,只松开A而按着ctrl,又接着按B,从而识别ctrl*B的组合键“问题
    private static int mouseWheelRecord = 0;// 滚轮计数器，1为单位。
    private int unit4Wheel = 1;// 滚轮计数单位。(可以调整为100)
    private Instant lastTime = Instant.now();// 记录开始时间
    private static ArrayList<Event> events = new ArrayList<>();// 记录所有事件


    private final AtomicInteger activeThreads = new AtomicInteger(0);// 线程活跃数计数器

    /*键盘监听*/
    public void nativeKeyPressed(NativeKeyEvent e) {
        String keyName = NativeKeyEvent.getKeyText(e.getKeyCode());

        // 判断组合键
        if (st4CombineKey.isEmpty()) {
            st4CombineKey.add(keyName);
        } else {
            st4CombineKey.add("*" + keyName);
        }

        System.out.println("Key Pressed: " + keyName);
    }

    public void nativeKeyReleased(NativeKeyEvent e) {
        if (mouseWheelRecord != 0) {// 笨办法实现滚轮记录
            this.recordEvents4MouseWheelMove();
        }

        if (!this.keyRecord.isEmpty()) {
            this.recordDifferTime();
            System.out.println("Key Released,Get Combine Key: " + this.keyRecord.toString());
            this.recordEvents('G', this.keyRecord.toString());
        }

        if (this.keyRecord.toString().equals("Ctrl*Shift*Q")) {// 退出写死了
            try {
//                this.saveAsJson();
                this.writeEventsToExcel("src/main/resources");
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException var3) {
                var3.printStackTrace();
            }
        }

        this.keyRecord.setLength(0);
    }

    /*鼠标监听*/
    public void nativeMousePressed(NativeMouseEvent e) {
    }

    public void nativeMouseClicked(NativeMouseEvent e) {
        System.out.println("Mouse Clicked: " + e.getClickCount());
    }

    public void nativeMouseReleased(NativeMouseEvent e) {
        if (mouseWheelRecord != 0) {// 笨办法实现滚轮记录
            recordEvents4MouseWheelMove();
        }

        recordDifferTime();//记录时间
        System.out.println("Mouse Released: " + e.getButton() + e.getX() + ", " + e.getY());

        //记录为Event
        int button = e.getButton();
        switch (button) {
            case 1:
                recordEvents(EventType.MOUSE_LEFT_CLICK, e.getX() + "*" + e.getY());
                break;
            case 2:
                recordEvents(EventType.MOUSE_RIGHT_CLICK, e.getX() + "*" + e.getY());
                break;
            case 3:
                recordEvents(EventType.MOUSE_MID_CLICK, e.getX() + "*" + e.getY());
                break;
        }
    }

    /*
     * 鼠标移动
     */
//    public void nativeMouseMoved(NativeMouseEvent e) {
//        System.out.println("Mouse Moved: " + e.getX() + ", " + e.getY());
//    }

    /*
     * 鼠标拖拽
     */
//    public void nativeMouseDragged(NativeMouseEvent e) {
//        System.out.println("Mouse Dragged: " + e.getX() + ", " + e.getY());
//    }

    /**
     * 鼠标滚轮
     *
     * @param e
     */
    public void nativeMouseWheelMoved(NativeMouseWheelEvent e) {
        if (e.getWheelRotation() == 1) {
            mouseWheelRecord += unit4Wheel;//向前
        } else if (e.getWheelRotation() == -1) {
            mouseWheelRecord -= unit4Wheel;//向后
        }
    }

    /**
     * 笨办法实现 鼠标滚轮记录
     */
    public void recordEvents4MouseWheelMove() {
        recordDifferTime();
        System.out.println("Mosue Wheel Moved: " + mouseWheelRecord);
        recordEvents(EventType.MOUSE_WHEEL_MOVE, mouseWheelRecord + "");
        //清除mouseWheelRecord
        mouseWheelRecord = 0;
    }


    public static void main(String[] args) {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            System.exit(1);
        }
        GlobalScreen.addNativeKeyListener(new YiZhiAutoListening());//开启键盘监听
        GlobalScreen.addNativeMouseListener(new YiZhiAutoListening());//开启鼠标监听
        GlobalScreen.addNativeMouseMotionListener(new YiZhiAutoListening());//开启鼠标钩子函数监听
        GlobalScreen.addNativeMouseWheelListener(new YiZhiAutoListening());//开启鼠标的滚轮监听
    }

//    /**
//     * 存储为json格式文件
//     */
//    public void saveAsJson() {
//        Gson gson = new Gson();
//        String json = gson.toJson(events);
//        System.out.println("======================\n" + json);
//    }

    /**
     * 记录为按键为Event对象
     *
     * @param type
     * @param detail
     */
    private void recordEvents(char type, String detail) {
        Event event = new Event(type, detail);
        events.add(event);
    }

    /**
     * 记录中间停顿时间
     */
    private void recordDifferTime() {
        char F_type = EventType.TIME_DIFF;
        Instant now = Instant.now();
        Duration duration = Duration.between(now, lastTime);
        long seconds = duration.getSeconds() * (-1);// 获取期间停顿的时间间隔
        recordEvents(F_type, seconds + "");
        lastTime = now;
    }

    /**
     * 序列化对象为excel格式
     */
    private void writeEventsToExcel(String outputPath) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Events");

        // Populate data rows
        int rowNum = 1;
        for (Event event : events) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(event.getType() + "");
            row.createCell(1).setCellValue(event.getDetail());
        }

        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }

        /// Write the output to a file
        try (FileOutputStream fileOut = new FileOutputStream(outputPath + "/events.xlsx")) {
            workbook.write(fileOut);
        } catch (Exception e) {
            e.printStackTrace();
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
