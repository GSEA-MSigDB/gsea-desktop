/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.shell;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Windows dark title bar via DWM (JavaFX CSS cannot style the native frame).
 */
public final class FxWindowsDarkFrame {

    private static final Logger klog = LoggerFactory.getLogger(FxWindowsDarkFrame.class);

    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1 = 19;
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_CAPTION_COLOR = 35;
    private static final int DWMWA_TEXT_COLOR = 36;
    private static final int DWMWA_COLOR_DEFAULT = 0xFFFFFFFF;
    /** #1e1f22 as COLORREF 0x00BBGGRR. */
    private static final int CAPTION_DARK = 0x00221F1E;
    private static final int TEXT_DARK = 0x00EDE8E8;

    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOZORDER = 0x0004;
    private static final int SWP_NOACTIVATE = 0x0010;
    private static final int SWP_FRAMECHANGED = 0x0020;

    private static final AtomicBoolean LOGGED_FAILURE = new AtomicBoolean();

    private FxWindowsDarkFrame() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /**
     * @return true if DWM attributes were applied; false if HWND was not ready yet (caller may retry)
     */
    public static boolean apply(Window window, boolean dark) {
        if (!isWindows() || window == null || !window.isShowing()) {
            return false;
        }
        try {
            Pointer hwnd = resolveHwnd(window);
            if (hwnd == null || Pointer.nativeValue(hwnd) == 0L) {
                return false;
            }
            IntByReference onOff = new IntByReference(dark ? 1 : 0);
            DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, onOff.getPointer(), 4);
            DwmApi.INSTANCE.DwmSetWindowAttribute(
                    hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE_BEFORE_20H1, onOff.getPointer(), 4);

            IntByReference caption = new IntByReference(dark ? CAPTION_DARK : DWMWA_COLOR_DEFAULT);
            IntByReference text = new IntByReference(dark ? TEXT_DARK : DWMWA_COLOR_DEFAULT);
            DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, caption.getPointer(), 4);
            DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_TEXT_COLOR, text.getPointer(), 4);

            User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED);
            return true;
        } catch (Throwable t) {
            logFailureOnce(t.toString());
            return false;
        }
    }

    private static void logFailureOnce(String detail) {
        if (LOGGED_FAILURE.compareAndSet(false, true)) {
            klog.warn("Windows dark title bar unavailable: {}", detail);
        }
    }

    private static Pointer resolveHwnd(Window window) {
        if (window instanceof Stage stage) {
            String title = stage.getTitle();
            if (title != null && !title.isBlank()) {
                Pointer hwnd = User32.INSTANCE.FindWindowW(null, new WString(title));
                if (hwnd != null && Pointer.nativeValue(hwnd) != 0L) {
                    return hwnd;
                }
            }
        }
        return hwndFromPeer(window);
    }

    /** Fallback via Glass peer (needs {@code --add-opens} for WindowHelper). */
    private static Pointer hwndFromPeer(Window window) {
        try {
            Class<?> helper = Class.forName("com.sun.javafx.stage.WindowHelper");
            Method getPeer = helper.getDeclaredMethod("getPeer", Window.class);
            getPeer.setAccessible(true);
            Object tkStage = getPeer.invoke(null, window);
            if (tkStage == null) {
                return null;
            }
            Method getRawHandle = tkStage.getClass().getMethod("getRawHandle");
            Object raw = getRawHandle.invoke(tkStage);
            if (raw instanceof Number n && n.longValue() != 0L) {
                return Pointer.createConstant(n.longValue());
            }
        } catch (Throwable ignored) {
            // Expected without add-opens; titled stages use FindWindow instead.
        }
        return null;
    }

    private interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

        int DwmSetWindowAttribute(Pointer hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    }

    private interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer FindWindowW(WString lpClassName, WString lpWindowName);

        boolean SetWindowPos(Pointer hWnd, Pointer hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags);
    }
}
