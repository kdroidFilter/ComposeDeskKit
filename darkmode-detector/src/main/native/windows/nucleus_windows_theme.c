/**
 * JNI bridge for Windows dark-mode detection and DWM title-bar theming.
 *
 * Provides native implementations for:
 *   - Reading the AppsUseLightTheme registry value
 *   - Monitoring registry changes (blocking)
 *
 * Linked libraries: advapi32.lib
 */

#include <jni.h>
#include <windows.h>

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
    (void)hinstDLL; (void)fdwReason; (void)lpvReserved;
    return TRUE;
}

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

