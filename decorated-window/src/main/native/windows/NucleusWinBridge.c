#include <jni.h>
#include <jawt.h>
#include <jawt_md.h>
#include <windows.h>
#include <dwmapi.h>

/*
 * Gets the HWND from a Java AWT Component using JAWT.
 * Returns the HWND on success, NULL on failure.
 */
static HWND getHWND(JNIEnv *env, jobject component, JAWT *awt) {
    if (!component || !awt) return NULL;

    /* Get the drawing surface for the component */
    JAWT_DrawingSurface *ds = awt->GetDrawingSurface(env, component);
    if (!ds) return NULL;

    jint lock = ds->Lock(ds);
    if (lock & JAWT_LOCK_ERROR) {
        awt->FreeDrawingSurface(ds);
        return NULL;
    }

    JAWT_DrawingSurfaceInfo *dsi = ds->GetDrawingSurfaceInfo(ds);
    if (!dsi || !dsi->platformInfo) {
        ds->Unlock(ds);
        awt->FreeDrawingSurface(ds);
        return NULL;
    }

    /* Extract the HWND from the platform-specific info */
    JAWT_Win32DrawingSurfaceInfo *win32dsi =
        (JAWT_Win32DrawingSurfaceInfo *)dsi->platformInfo;
    HWND hwnd = win32dsi->hwnd;

    ds->FreeDrawingSurfaceInfo(dsi);
    ds->Unlock(ds);
    awt->FreeDrawingSurface(ds);

    return hwnd;
}

/*
 * Uses JAWT to obtain the native HWND from an AWT Component,
 * then uses SetWindowPos with SWP_SHOWWINDOW | SWP_MAXIMIZE 
 * to show the window already maximized - no animation, instant maximize.
 *
 * This is more aggressive than ShowWindow(SW_SHOWMAXIMIZED) because
 * it combines showing and maximizing in one call.
 *
 * Returns JNI_TRUE on success, JNI_FALSE on failure.
 */
JNIEXPORT jboolean JNICALL
Java_io_github_kdroidfilter_nucleus_window_utils_windows_NativeWinBridge_nativeShowMaximized(
    JNIEnv *env, jclass cls, jobject component) {

    if (!component) return JNI_FALSE;

    /* Obtain JAWT interface */
    JAWT awt;
    awt.version = JAWT_VERSION_1_4;
    if (!JAWT_GetAWT(env, &awt)) return JNI_FALSE;

    /* Get HWND */
    HWND hwnd = getHWND(env, component, &awt);
    if (!hwnd || !IsWindow(hwnd)) return JNI_FALSE;

    /* 
     * First set WS_MAXIMIZE style, then show maximized.
     * This ensures Windows knows the window is maximized before showing.
     * DWM transitions are enabled for smooth animation effect.
     */
    SetWindowLongPtr(hwnd, GWL_STYLE, 
                     GetWindowLongPtr(hwnd, GWL_STYLE) | WS_MAXIMIZE);
    SetWindowPos(hwnd, HWND_TOP, 0, 0, 0, 0,
                 SWP_NOSIZE | SWP_NOMOVE | SWP_FRAMECHANGED);
    ShowWindow(hwnd, SW_SHOWMAXIMIZED);

    return JNI_TRUE;
}

/*
 * Creates a window in maximized state using Win32 API directly.
 * This is called before the window is shown to set its initial state.
 * 
 * Returns JNI_TRUE on success, JNI_FALSE on failure.
 */
JNIEXPORT jboolean JNICALL
Java_io_github_kdroidfilter_nucleus_window_utils_windows_NativeWinBridge_nativeCreateMaximized(
    JNIEnv *env, jclass cls, jobject component) {

    if (!component) return JNI_FALSE;

    /* Obtain JAWT interface */
    JAWT awt;
    awt.version = JAWT_VERSION_1_4;
    if (!JAWT_GetAWT(env, &awt)) return JNI_FALSE;

    /* Get HWND */
    HWND hwnd = getHWND(env, component, &awt);
    if (!hwnd || !IsWindow(hwnd)) return JNI_FALSE;

    /* 
     * Set window style to include WS_MAXIMIZE
     * This makes the window start maximized
     */
    SetWindowLongPtr(hwnd, GWL_STYLE, 
                     GetWindowLongPtr(hwnd, GWL_STYLE) | WS_MAXIMIZE);

    /* Use SetWindowPos to apply the maximized state */
    SetWindowPos(hwnd, HWND_TOP, 0, 0, 0, 0,
                 SWP_NOSIZE | SWP_NOMOVE | SWP_FRAMECHANGED);

    return JNI_TRUE;
}

/*
 * Brings the window to foreground using Win32 SetForegroundWindow.
 * This is more reliable than toFront() on Windows.
 * 
 * Uses multiple attempts and different methods to ensure success.
 *
 * Returns JNI_TRUE on success, JNI_FALSE on failure.
 */
JNIEXPORT jboolean JNICALL
Java_io_github_kdroidfilter_nucleus_window_utils_windows_NativeWinBridge_nativeBringToForeground(
    JNIEnv *env, jclass cls, jobject component) {

    if (!component) return JNI_FALSE;

    /* Obtain JAWT interface */
    JAWT awt;
    awt.version = JAWT_VERSION_1_4;
    if (!JAWT_GetAWT(env, &awt)) return JNI_FALSE;

    /* Get HWND */
    HWND hwnd = getHWND(env, component, &awt);
    if (!hwnd || !IsWindow(hwnd)) return JNI_FALSE;

    /* Check if window is minimized and restore if needed */
    if (IsIconic(hwnd)) {
        ShowWindow(hwnd, SW_RESTORE);
    }

    /* 
     * Try multiple methods to bring to foreground:
     * 1. SetForegroundWindow (most reliable)
     * 2. SetActiveWindow (fallback)
     * 3. BringWindowToTop
     */
    
    /* First, try to attach to the current foreground thread */
    DWORD foregroundThreadId = GetWindowThreadProcessId(GetForegroundWindow(), NULL);
    DWORD currentThreadId = GetCurrentThreadId();
    
    if (foregroundThreadId != currentThreadId) {
        AttachThreadInput(foregroundThreadId, currentThreadId, TRUE);
    }

    /* Try SetForegroundWindow */
    BOOL success = SetForegroundWindow(hwnd);
    
    if (!success) {
        /* Fallback: try SetActiveWindow */
        SetActiveWindow(hwnd);
    }

    /* Also try BringWindowToTop */
    BringWindowToTop(hwnd);

    /* Detach thread input if we attached it */
    if (foregroundThreadId != currentThreadId) {
        AttachThreadInput(foregroundThreadId, currentThreadId, FALSE);
    }

    /* Make sure window is visible */
    ShowWindow(hwnd, SW_SHOW);

    return JNI_TRUE;
}
