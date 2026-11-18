/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/** Reads whether the operating system is using a dark appearance. */
final class FxOsAppearance {
    private FxOsAppearance() {}

    static Boolean detect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                return detectWindowsDark();
            }
            if (os.contains("mac")) {
                return detectMacDark();
            }
            return detectLinuxDark();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Boolean detectWindowsDark() {
        Boolean viaJna = detectWindowsDarkViaRegistry();
        if (viaJna != null) {
            return viaJna;
        }
        try {
            Process p = new ProcessBuilder(
                    "reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme")
                    .redirectErrorStream(true)
                    .start();
            String out = readProcess(p, 1);
            for (String line : out.split("\\R")) {
                String t = line.trim().toLowerCase(Locale.ROOT);
                if (!t.contains("appsuselighttheme")) {
                    continue;
                }
                if (t.contains("0x0")) {
                    return Boolean.TRUE;
                }
                if (t.contains("0x1")) {
                    return Boolean.FALSE;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Fast Advapi32 read of AppsUseLightTheme (0 = dark apps). Avoids spawning reg.exe on the FX thread.
     */
    private static Boolean detectWindowsDarkViaRegistry() {
        try {
            PointerByReference phkResult = new PointerByReference();
            int open = Advapi32.INSTANCE.RegOpenKeyEx(
                    Advapi32.HKEY_CURRENT_USER,
                    "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    0,
                    Advapi32.KEY_READ,
                    phkResult);
            if (open != 0) {
                return null;
            }
            Pointer hKey = phkResult.getValue();
            try {
                IntByReference type = new IntByReference();
                Memory data = new Memory(4);
                IntByReference size = new IntByReference(4);
                int q = Advapi32.INSTANCE.RegQueryValueEx(
                        hKey,
                        "AppsUseLightTheme",
                        null,
                        type,
                        data,
                        size);
                if (q != 0 || type.getValue() != Advapi32.REG_DWORD) {
                    return null;
                }
                // 0 = dark, 1 = light
                return data.getInt(0) == 0;
            } finally {
                Advapi32.INSTANCE.RegCloseKey(hKey);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static Boolean detectMacDark() throws Exception {
        Process p = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
                .redirectErrorStream(true)
                .start();
        String out = readProcess(p, 1).trim();
        // Missing AppleInterfaceStyle means light appearance.
        return out.equalsIgnoreCase("Dark");
    }

    private static Boolean detectLinuxDark() throws Exception {
        Process p = new ProcessBuilder(
                "gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
                .redirectErrorStream(true)
                .start();
        String out = readProcess(p, 1).toLowerCase(Locale.ROOT);
        if (out.contains("prefer-dark")) {
            return Boolean.TRUE;
        }
        if (out.contains("prefer-light")) {
            return Boolean.FALSE;
        }
        Process p2 = new ProcessBuilder(
                "gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")
                .redirectErrorStream(true)
                .start();
        String theme = readProcess(p2, 1).toLowerCase(Locale.ROOT);
        if (theme.isBlank()) {
            return null;
        }
        return theme.contains("-dark");
    }

    private static String readProcess(Process p, int timeoutSec) throws Exception {
        boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /** Minimal Advapi32 for reading the Windows apps theme DWORD. */
    private interface Advapi32 extends StdCallLibrary {
        Advapi32 INSTANCE = Native.load("advapi32", Advapi32.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer HKEY_CURRENT_USER = Pointer.createConstant(0x80000001L);
        int KEY_READ = 0x20019;
        int REG_DWORD = 4;

        int RegOpenKeyEx(Pointer hKey, String lpSubKey, int ulOptions, int samDesired,
                PointerByReference phkResult);

        int RegQueryValueEx(Pointer hKey, String lpValueName, IntByReference lpReserved, IntByReference lpType,
                Pointer lpData, IntByReference lpcbData);

        int RegCloseKey(Pointer hKey);
    }
}
