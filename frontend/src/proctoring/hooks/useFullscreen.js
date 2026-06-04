import { useCallback, useEffect, useState } from 'react';

/**
 * Hook wrapping the Fullscreen API.
 *
 * Tracks whether ANY element (not just targetRef) is in fullscreen.
 * This allows the Entry page to request fullscreen on documentElement
 * and the Arena to detect that state without needing its own ref to
 * be the fullscreen element.
 *
 * @param {React.RefObject<HTMLElement>} [targetRef] - ref to the
 *   element to request fullscreen on (for enter()). Defaults to
 *   document.documentElement if omitted.
 * @returns {{ isFullscreen: boolean, enter: () => Promise<void>, exit: () => Promise<void> }}
 */
export function useFullscreen(targetRef) {
  const isAnyFullscreen = useCallback(() => {
    if (typeof document === 'undefined') return false;
    return !!(document.fullscreenElement || document.webkitFullscreenElement);
  }, []);

  // Start false. The effect below reconciles immediately on mount.
  const [isFullscreen, setIsFullscreen] = useState(false);

  const enter = useCallback(async () => {
    const el = (targetRef && targetRef.current) || document.documentElement;
    if (!el) return;
    if (isAnyFullscreen()) return;
    const request =
      el.requestFullscreen ||
      el.webkitRequestFullscreen ||
      el.msRequestFullscreen;
    if (!request) return;
    try {
      await request.call(el);
    } catch {
      // User gesture / API rejection.
    }
  }, [targetRef, isAnyFullscreen]);

  const exit = useCallback(async () => {
    if (typeof document === 'undefined') return;
    if (!document.fullscreenElement && !document.webkitFullscreenElement) return;
    const exitFn =
      document.exitFullscreen ||
      document.webkitExitFullscreen ||
      document.msExitFullscreen;
    if (!exitFn) return;
    try {
      await exitFn.call(document);
    } catch {
      // Ignore — change event will reconcile.
    }
  }, []);

  useEffect(() => {
    if (typeof document === 'undefined') return undefined;
    const onChange = () => setIsFullscreen(isAnyFullscreen());
    document.addEventListener('fullscreenchange', onChange);
    document.addEventListener('webkitfullscreenchange', onChange);
    // Sync once on mount.
    onChange();
    return () => {
      document.removeEventListener('fullscreenchange', onChange);
      document.removeEventListener('webkitfullscreenchange', onChange);
    };
  }, [isAnyFullscreen]);

  return { isFullscreen, enter, exit };
}

export default useFullscreen;
