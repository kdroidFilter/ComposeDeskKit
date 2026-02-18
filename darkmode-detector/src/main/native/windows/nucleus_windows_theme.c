/**
 * JNI bridge for Windows dark-mode detection and DWM title-bar theming.
 *
 * Provides native implementations for:
 *   - Reading the AppsUseLightTheme registry value
 *   - Monitoring registry changes (blocking)
 *   - Applying immersive dark-mode to a window title bar via DWM
 *
 * Linked libraries: advapi32.lib, dwmapi.lib, jawt.lib
 */

#include <jni.h>
#include <windows.h>
#include <dwmapi.h>
#include <jawt.h>
#include <jawt_md.h>

#ifndef DWMWA_USE_IMMERSIVE_DARK_MODE
#define DWMWA_USE_IMMERSIVE_DARK_MODE 20
#endif

static const char *REG_PATH =
    "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
static const char *REG_VALUE = "AppsUseLightTheme";

/* ------------------------------------------------------------------ */
/*  nativeIsDark()                                                     */
/* ------------------------------------------------------------------ */
JNIEXPORT jboolean JNICALL
Java_io_github_kdroidfilter_nucleus_darkmodedetector_windows_NativeWindowsBridge_nativeIsDark(
    JNIEnv *env, jclass clazz)
{
    DWORD value = 1; /* default: light */
    DWORD size = sizeof(value);
    LONG err = RegGetValueA(
        HKEY_CURRENT_USER, REG_PATH, REG_VALUE,
        RRF_RT_REG_DWORD, NULL, &value, &size);

    if (err != ERROR_SUCCESS) {
        return JNI_FALSE; /* key absent → light */
    }
    return (value == 0) ? JNI_TRUE : JNI_FALSE;
}

/* ------------------------------------------------------------------ */
/*  nativeOpenMonitorKey() → jlong (HKEY handle)                       */
/* ------------------------------------------------------------------ */
JNIEXPORT jlong JNICALL
Java_io_github_kdroidfilter_nucleus_darkmodedetector_windows_NativeWindowsBridge_nativeOpenMonitorKey(
    JNIEnv *env, jclass clazz)
{
    HKEY hKey = NULL;
    LONG err = RegOpenKeyExA(
        HKEY_CURRENT_USER, REG_PATH, 0, KEY_READ, &hKey);
    if (err != ERROR_SUCCESS) {
        return 0;
    }
    return (jlong)(uintptr_t)hKey;
}

/* ------------------------------------------------------------------ */
/*  nativeWaitForChange(jlong hKey) → jboolean success                 */
/* ------------------------------------------------------------------ */
JNIEXPORT jboolean JNICALL
Java_io_github_kdroidfilter_nucleus_darkmodedetector_windows_NativeWindowsBridge_nativeWaitForChange(
    JNIEnv *env, jclass clazz, jlong hKey)
{
    LONG err = RegNotifyChangeKeyValue(
        (HKEY)(uintptr_t)hKey,
        FALSE,
        REG_NOTIFY_CHANGE_LAST_SET,
        NULL,
        FALSE);
    return (err == ERROR_SUCCESS) ? JNI_TRUE : JNI_FALSE;
}

/* ------------------------------------------------------------------ */
/*  nativeCloseKey(jlong hKey)                                         */
/* ------------------------------------------------------------------ */
JNIEXPORT void JNICALL
Java_io_github_kdroidfilter_nucleus_darkmodedetector_windows_NativeWindowsBridge_nativeCloseKey(
    JNIEnv *env, jclass clazz, jlong hKey)
{
    if (hKey != 0) {
        RegCloseKey((HKEY)(uintptr_t)hKey);
    }
}

/* ------------------------------------------------------------------ */
/*  nativeSetDarkModeTitleBar(Component, boolean dark)                  */
/* ------------------------------------------------------------------ */
JNIEXPORT jboolean JNICALL
Java_io_github_kdroidfilter_nucleus_darkmodedetector_windows_NativeWindowsBridge_nativeSetDarkModeTitleBar(
    JNIEnv *env, jclass clazz, jobject awtComponent, jboolean dark)
{
    JAWT awt;
    awt.version = JAWT_VERSION_1_4;
    if (!JAWT_GetAWT(env, &awt)) {
        return JNI_FALSE;
    }

    JAWT_DrawingSurface *ds = awt.GetDrawingSurface(env, awtComponent);
    if (ds == NULL) {
        return JNI_FALSE;
    }

    jint lock = ds->Lock(ds);
    if ((lock & JAWT_LOCK_ERROR) != 0) {
        awt.FreeDrawingSurface(ds);
        return JNI_FALSE;
    }

    JAWT_DrawingSurfaceInfo *dsi = ds->GetDrawingSurfaceInfo(ds);
    if (dsi == NULL) {
        ds->Unlock(ds);
        awt.FreeDrawingSurface(ds);
        return JNI_FALSE;
    }

    JAWT_Win32DrawingSurfaceInfo *win32dsi =
        (JAWT_Win32DrawingSurfaceInfo *)dsi->platformInfo;
    HWND hwnd = win32dsi->hwnd;

    ds->FreeDrawingSurfaceInfo(dsi);
    ds->Unlock(ds);
    awt.FreeDrawingSurface(ds);

    BOOL useDark = dark ? TRUE : FALSE;
    HRESULT hr = DwmSetWindowAttribute(
        hwnd,
        DWMWA_USE_IMMERSIVE_DARK_MODE,
        &useDark,
        sizeof(useDark));

    return SUCCEEDED(hr) ? JNI_TRUE : JNI_FALSE;
}
