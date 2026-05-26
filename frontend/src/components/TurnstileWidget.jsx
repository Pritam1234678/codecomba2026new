import { useEffect, useRef, useId } from 'react';

const SITE_KEY = import.meta.env.VITE_TURNSTILE_SITE_KEY;

/**
 * Cloudflare Turnstile widget wrapper. Renders a Turnstile challenge and
 * surfaces the resolved token via `onToken(token)` callback. Pass an `onExpire`
 * to clear the parent's token state when the token times out (5 min).
 *
 * Uses the explicit-render API so we get a stable widgetId and can force-reset
 * it after a failed form submission via the imperative `reset()` method
 * exposed through the `widgetIdRef`.
 *
 * Theme: defaults to 'dark' since the platform is dark.
 */
const TurnstileWidget = ({ onToken, onExpire, widgetIdRef, theme = 'dark', size = 'normal' }) => {
    const containerRef = useRef(null);
    const localIdRef = useRef(null);
    const reactId = useId();

    useEffect(() => {
        let cancelled = false;
        if (!SITE_KEY) {
            console.error('VITE_TURNSTILE_SITE_KEY is not set — Turnstile widget will not render');
            return;
        }

        const render = () => {
            if (cancelled || !containerRef.current || !window.turnstile) return;
            try {
                const id = window.turnstile.render(containerRef.current, {
                    sitekey: SITE_KEY,
                    theme,
                    size,
                    callback: (token) => onToken?.(token),
                    'expired-callback': () => onExpire?.(),
                    'error-callback': () => onExpire?.(),
                });
                localIdRef.current = id;
                if (widgetIdRef) widgetIdRef.current = id;
            } catch (e) {
                console.error('Turnstile render failed', e);
            }
        };

        let pollHandle = null;
        if (window.turnstile && typeof window.turnstile.render === 'function') {
            render();
        } else {
            // Script still loading — poll until ready
            const start = Date.now();
            pollHandle = setInterval(() => {
                if (window.turnstile?.render) {
                    clearInterval(pollHandle);
                    pollHandle = null;
                    render();
                } else if (Date.now() - start > 10000) {
                    clearInterval(pollHandle);
                    pollHandle = null;
                    console.error('Turnstile script failed to load within 10s');
                }
            }, 100);
        }

        return () => {
            cancelled = true;
            if (pollHandle) clearInterval(pollHandle);
            const id = localIdRef.current;
            if (id != null && window.turnstile?.remove) {
                try { window.turnstile.remove(id); } catch { /* ignore */ }
            }
        };
    }, [reactId]); // eslint-disable-line react-hooks/exhaustive-deps

    return <div ref={containerRef} style={{ minHeight: 65 }} />;
};

export default TurnstileWidget;
