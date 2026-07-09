import { useEffect, useRef } from 'react';

const MONACO_SELECTOR = '.monaco-editor';

const EVENT_TYPE_MAP = Object.freeze({
  copy: 'COPY_ATTEMPT',
  cut: 'CUT_ATTEMPT',
  paste: 'PASTE_ATTEMPT',
  contextmenu: 'CONTEXT_MENU_ATTEMPT',
});

const DOM_EVENT_NAMES = Object.freeze(['copy', 'cut', 'paste', 'contextmenu']);

const NO_SELECT_CLASS = 'proctoring-no-select';
const STYLE_ELEMENT_ID = 'proctoring-no-select-style';

/**
 * Injects the stylesheet. When `blockEditor` is true, the Monaco carve-out
 * is removed so `user-select: none` applies EVERYWHERE.
 */
function ensureNoSelectStylesheet(blockEditor) {
  if (typeof document === 'undefined') return null;

  // Remove the old one so we can switch between blockEditor modes.
  const old = document.getElementById(STYLE_ELEMENT_ID);
  if (old) old.remove();

  const css = blockEditor
    ? `.${NO_SELECT_CLASS},.${NO_SELECT_CLASS} * {
         -webkit-user-select:none!important;-moz-user-select:none!important;
         -ms-user-select:none!important;user-select:none!important;
       }`
    : `.${NO_SELECT_CLASS},
       .${NO_SELECT_CLASS} *:not(.monaco-editor):not(.monaco-editor *) {
         -webkit-user-select:none!important;-moz-user-select:none!important;
         -ms-user-select:none!important;user-select:none!important;
       }
       .${NO_SELECT_CLASS} .monaco-editor,
       .${NO_SELECT_CLASS} .monaco-editor * {
         -webkit-user-select:text!important;-moz-user-select:text!important;
         -ms-user-select:text!important;user-select:text!important;
       }`;

  const el = document.createElement('style');
  el.id = STYLE_ELEMENT_ID;
  el.type = 'text/css';
  el.appendChild(document.createTextNode(css));
  document.head.appendChild(el);
  return el;
}

function isMonacoOriginatedEvent(event) {
  const target = event && event.target;
  if (!target || typeof target.closest !== 'function') return false;
  return target.closest(MONACO_SELECTOR) !== null;
}

/**
 * @param {object} options
 * @param {React.RefObject<HTMLElement>} [options.rootRef]
 * @param {(eventType: string) => void} [options.onEvent]
 * @param {boolean} [options.enabled=true]
 * @param {boolean} [options.reportEvents=true]
 * @param {boolean} [options.blockEditor=false] — when true, also block inside Monaco
 */
export function useCopyPasteBlocker({
  rootRef,
  onEvent,
  enabled = true,
  reportEvents = true,
  blockEditor = false,
} = {}) {
  const onEventRef = useRef(onEvent);
  useEffect(() => { onEventRef.current = onEvent; }, [onEvent]);

  useEffect(() => {
    if (!enabled || typeof document === 'undefined') return undefined;

    const rootEl = rootRef && rootRef.current ? rootRef.current : null;
    const listenerTarget = rootEl || document;
    const classTarget = rootEl || document.body;

    ensureNoSelectStylesheet(blockEditor);

    if (classTarget && classTarget.classList) {
      classTarget.classList.add(NO_SELECT_CLASS);
    }

    const handler = (event) => {
      const eventType = EVENT_TYPE_MAP[event.type];
      if (!eventType) return;
      // When blockEditor is false, skip Monaco so in-editor copy/paste works.
      // When blockEditor is true, block EVERYTHING including inside Monaco.
      if (!blockEditor && isMonacoOriginatedEvent(event)) return;
      event.preventDefault();
      if (reportEvents) {
        const cb = onEventRef.current;
        if (typeof cb === 'function') {
          try { cb(eventType); } catch { /* ignore */ }
        }
      }
    };

    const opts = { capture: true };
    DOM_EVENT_NAMES.forEach((name) => {
      listenerTarget.addEventListener(name, handler, opts);
    });

    return () => {
      DOM_EVENT_NAMES.forEach((name) => {
        listenerTarget.removeEventListener(name, handler, opts);
      });
      if (classTarget && classTarget.classList) {
        classTarget.classList.remove(NO_SELECT_CLASS);
      }
      const styleEl = document.getElementById(STYLE_ELEMENT_ID);
      if (styleEl) styleEl.remove();
    };
  }, [rootRef, enabled, reportEvents, blockEditor]);
}

export default useCopyPasteBlocker;
