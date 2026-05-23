import { useState, useEffect, useRef, useCallback } from 'react';
import Editor from '@monaco-editor/react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';

// ── Theme tokens ──────────────────────────────────────────────────────────────
const C = {
    bg:         '#131313',
    surfaceCon: '#201f1f',
    surfaceLow: '#1c1b1b',
    surfaceHi:  '#2a2a2a',
    surfaceMin: '#0e0e0e',
    border:     '#50453b',
    primary:    '#f1bc8b',
    secondary:  '#e9c176',
    muted:      '#d4c4b7',
    outline:    '#9d8e83',
    onBg:       '#e5e2e1',
    error:      '#ffb4ab',
    success:    '#4ade80',
};

const LANG_MAP = {
    JAVA:       { label: 'Java 21',     monaco: 'java',       starter: 'import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        System.out.print("Enter your name: ");\n        String name = sc.nextLine();\n        System.out.println("Hello, " + name + "!");\n    }\n}\n' },
    CPP:        { label: 'C++ 20',      monaco: 'cpp',        starter: '#include <iostream>\nusing namespace std;\n\nint main() {\n    string name;\n    cout << "Enter your name: ";\n    getline(cin, name);\n    cout << "Hello, " << name << "!" << endl;\n    return 0;\n}\n' },
    C:          { label: 'C',           monaco: 'c',          starter: '#include <stdio.h>\n\nint main() {\n    char name[100];\n    printf("Enter your name: ");\n    fgets(name, sizeof(name), stdin);\n    printf("Hello, %s", name);\n    return 0;\n}\n' },
    PYTHON:     { label: 'Python 3.11', monaco: 'python',     starter: 'name = input("Enter your name: ")\nprint(f"Hello, {name}!")\n' },
    JAVASCRIPT: { label: 'JavaScript',  monaco: 'javascript', starter: 'const readline = require("readline");\nconst rl = readline.createInterface({ input: process.stdin, output: process.stdout });\nrl.question("Enter your name: ", (name) => {\n    console.log(`Hello, ${name}!`);\n    rl.close();\n});\n' },
};

// Terminal theme matching site palette
const TERM_THEME = {
    background: C.surfaceMin,
    foreground: C.onBg,
    cursor:     C.secondary,
    cursorAccent: C.bg,
    selectionBackground: 'rgba(233,193,118,0.25)',
    black: '#1c1b1b',
    red: '#ffb4ab',
    green: '#4ade80',
    yellow: '#facc15',
    blue: '#7ab3e0',
    magenta: '#a78bfa',
    cyan: '#67e8f9',
    white: '#e5e2e1',
    brightBlack: '#50453b',
    brightRed: '#ffb4ab',
    brightGreen: '#4ade80',
    brightYellow: '#e9c176',
    brightBlue: '#7ab3e0',
    brightMagenta: '#a78bfa',
    brightCyan: '#67e8f9',
    brightWhite: '#f1bc8b',
};

const CoderCompiler = () => {
    const [language, setLanguage] = useState('PYTHON');
    const [code, setCode]         = useState(LANG_MAP.PYTHON.starter);
    const [running, setRunning]   = useState(false);

    // Refs
    const termRef    = useRef(null);   // DOM element
    const termObjRef = useRef(null);   // xterm Terminal instance
    const fitRef     = useRef(null);
    const wsRef      = useRef(null);
    const lineBufferRef = useRef('');  // current line being typed (for backspace handling)

    // Vertical split: left (editor) vs right (terminal)
    const [leftWidth, setLeftWidth] = useState(58);
    const [isDrag, setIsDrag] = useState(false);

    // ── Initialize xterm.js once ──────────────────────────────────────────────
    useEffect(() => {
        if (!termRef.current) return;

        const term = new Terminal({
            theme: TERM_THEME,
            fontFamily: '"JetBrains Mono", "Fira Code", "Cascadia Code", monospace',
            fontSize: 13,
            lineHeight: 1.4,
            cursorBlink: true,
            cursorStyle: 'block',
            scrollback: 5000,
            allowProposedApi: true,
            convertEol: true,
        });
        const fit = new FitAddon();
        term.loadAddon(fit);
        term.open(termRef.current);
        fit.fit();

        termObjRef.current = term;
        fitRef.current = fit;

        term.writeln('\x1b[38;5;180mCoderCompiler — Interactive Terminal\x1b[0m');
        term.writeln('\x1b[38;5;245mClick "Run" to start your program. You can type input directly here when prompted.\x1b[0m');
        term.writeln('');

        // Handle user keystrokes — send to WebSocket if running
        term.onData((data) => {
            if (wsRef.current?.readyState === WebSocket.OPEN) {
                // Local echo for typed chars + handle backspace/enter
                if (data === '\r') {
                    // Enter — send full line + \n
                    term.write('\r\n');
                    wsRef.current.send(JSON.stringify({ type: 'input', data: lineBufferRef.current + '\n' }));
                    lineBufferRef.current = '';
                } else if (data === '\u007f') {
                    // Backspace
                    if (lineBufferRef.current.length > 0) {
                        lineBufferRef.current = lineBufferRef.current.slice(0, -1);
                        term.write('\b \b');
                    }
                } else if (data === '\u0003') {
                    // Ctrl+C — kill the process
                    wsRef.current.send(JSON.stringify({ type: 'kill' }));
                    term.writeln('\r\n\x1b[38;5;203m^C — process killed\x1b[0m');
                    lineBufferRef.current = '';
                } else if (data >= ' ' || data === '\t') {
                    // Printable
                    lineBufferRef.current += data;
                    term.write(data);
                }
            }
        });

        // Window resize → fit
        const onResize = () => { try { fit.fit(); } catch (e) {} };
        window.addEventListener('resize', onResize);

        return () => {
            window.removeEventListener('resize', onResize);
            try { term.dispose(); } catch (e) {}
        };
    }, []);

    // Re-fit terminal when split changes
    useEffect(() => {
        if (fitRef.current) {
            setTimeout(() => { try { fitRef.current.fit(); } catch (e) {} }, 50);
        }
    }, [leftWidth, isDrag]);

    const handleLanguageChange = (lang) => {
        setLanguage(lang);
        setCode(LANG_MAP[lang].starter);
    };

    // ── Run via WebSocket ─────────────────────────────────────────────────────
    const handleRun = useCallback(async () => {
        if (running) return;
        const term = termObjRef.current;
        if (!term) return;

        if (wsRef.current) {
            try { wsRef.current.close(); } catch (e) {}
            wsRef.current = null;
        }

        term.clear();
        term.writeln('\x1b[38;5;245m── Starting ' + LANG_MAP[language].label + ' ──\x1b[0m');
        lineBufferRef.current = '';

        // Fetch a single-use WS ticket if the user is logged in. Anonymous
        // visitors connect without a ticket and hit the public rate limit.
        let ticket = null;
        const stored = JSON.parse(localStorage.getItem('user') || 'null');
        if (stored?.token) {
            try {
                const res = await fetch('/api/compiler/ws-ticket', {
                    method: 'POST',
                    headers: { 'Authorization': 'Bearer ' + stored.token },
                });
                if (res.ok) {
                    const body = await res.json();
                    ticket = body?.ticket || null;
                }
            } catch (err) {
                // Non-fatal: fall through as anonymous
                console.warn('Failed to fetch WS ticket; connecting anonymously', err);
            }
        }

        const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.hostname === 'localhost'
            ? 'localhost:8080'
            : window.location.host;
        const ticketParam = ticket ? `?ticket=${encodeURIComponent(ticket)}` : '';
        const wsUrl = `${proto}//${host}/api/compiler/ws${ticketParam}`;

        const ws = new WebSocket(wsUrl);
        wsRef.current = ws;
        setRunning(true);

        ws.onopen = () => {
            ws.send(JSON.stringify({ type: 'start', language, code }));
        };

        ws.onmessage = (ev) => {
            try {
                const msg = JSON.parse(ev.data);
                switch (msg.type) {
                    case 'ready':
                        // optional: term.writeln('\x1b[38;5;245m── Ready ──\x1b[0m');
                        break;
                    case 'output':
                        term.write(msg.data);
                        break;
                    case 'stderr':
                        // Render stderr in red
                        term.write('\x1b[31m' + msg.data + '\x1b[0m');
                        break;
                    case 'error':
                        term.writeln('\r\n\x1b[31m' + msg.data + '\x1b[0m');
                        break;
                    case 'exit':
                        term.writeln('');
                        term.writeln('\x1b[38;5;245m── Process exited with code ' + msg.data + ' ──\x1b[0m');
                        setRunning(false);
                        break;
                    default:
                        break;
                }
            } catch (e) {
                console.error('WS parse error:', e);
            }
        };

        ws.onerror = () => {
            term.writeln('\r\n\x1b[31m── Connection error ──\x1b[0m');
            setRunning(false);
        };

        ws.onclose = () => {
            setRunning(false);
        };
    }, [code, language, running]);

    const handleStop = () => {
        if (wsRef.current?.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify({ type: 'kill' }));
            try { wsRef.current.close(); } catch (e) {}
        }
        setRunning(false);
    };

    // Cleanup on unmount
    useEffect(() => {
        return () => {
            if (wsRef.current) {
                try { wsRef.current.close(); } catch (e) {}
            }
        };
    }, []);

    // Ctrl+Enter to run
    useEffect(() => {
        const onKey = (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                handleRun();
            }
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [handleRun]);

    // Vertical divider drag
    const onDragStart = (e) => {
        e.preventDefault();
        setIsDrag(true);
        const startX = e.clientX;
        const startW = leftWidth;
        const onMove = (ev) => {
            const container = document.getElementById('cc-workspace');
            if (!container) return;
            const totalW = container.getBoundingClientRect().width;
            const delta = ev.clientX - startX;
            const newPct = Math.min(80, Math.max(20, startW + (delta / totalW) * 100));
            setLeftWidth(newPct);
        };
        const onUp = () => {
            setIsDrag(false);
            window.removeEventListener('mousemove', onMove);
            window.removeEventListener('mouseup', onUp);
        };
        window.addEventListener('mousemove', onMove);
        window.addEventListener('mouseup', onUp);
    };

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, height: '100vh', maxHeight: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden', fontFamily: "'Geist', sans-serif", userSelect: isDrag ? 'none' : 'auto' }}>

            {/* ── Header ── */}
            <header style={{ height: '56px', flexShrink: 0, borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceMin, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <span className="material-symbols-outlined" style={{ color: C.secondary, fontSize: '24px' }}>terminal</span>
                    <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 600, color: C.primary, margin: 0 }}>
                        CoderCompiler
                    </h1>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>
                        Interactive Sandbox
                    </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', border: `1px solid ${C.border}`, padding: '6px 12px' }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.outline }}>code_blocks</span>
                        <select
                            value={language}
                            onChange={e => handleLanguageChange(e.target.value)}
                            disabled={running}
                            style={{ backgroundColor: 'transparent', border: 'none', color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.08em', textTransform: 'uppercase', cursor: running ? 'not-allowed' : 'pointer', outline: 'none', appearance: 'none', opacity: running ? 0.5 : 1 }}
                        >
                            {Object.entries(LANG_MAP).map(([key, { label }]) => (
                                <option key={key} value={key} style={{ backgroundColor: C.surfaceLow }}>{label}</option>
                            ))}
                        </select>
                    </div>

                    {running ? (
                        <button
                            onClick={handleStop}
                            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 24px', border: `1px solid ${C.error}`, color: C.error, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s' }}
                            onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.error; e.currentTarget.style.color = C.bg; }}
                            onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.error; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '16px', fontVariationSettings: "'FILL' 1" }}>stop</span>
                            Stop
                        </button>
                    ) : (
                        <button
                            onClick={handleRun}
                            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 24px', border: `1px solid ${C.secondary}`, color: C.secondary, backgroundColor: 'transparent', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s' }}
                            onMouseEnter={e => { e.currentTarget.style.backgroundColor = C.secondary; e.currentTarget.style.color = C.bg; }}
                            onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = C.secondary; }}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '16px', fontVariationSettings: "'FILL' 1" }}>play_arrow</span>
                            Run
                        </button>
                    )}

                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, marginLeft: '8px' }}>
                        Ctrl+Enter
                    </span>
                </div>
            </header>

            {/* ── Workspace: side-by-side ── */}
            <main id="cc-workspace" style={{ flex: 1, display: 'flex', overflow: 'hidden', cursor: isDrag ? 'col-resize' : 'auto' }}>

                {/* Left: Editor */}
                <section style={{ width: `${leftWidth}%`, flexShrink: 0, display: 'flex', flexDirection: 'column', backgroundColor: C.bg, minWidth: 0 }}>
                    <Editor
                        height="100%"
                        theme="vs-dark"
                        language={LANG_MAP[language]?.monaco || 'python'}
                        value={code}
                        onChange={v => setCode(v || '')}
                        options={{
                            fontSize: 14,
                            fontFamily: "'Fira Code', 'Cascadia Code', 'JetBrains Mono', monospace",
                            fontLigatures: true,
                            minimap: { enabled: false },
                            scrollBeyondLastLine: false,
                            automaticLayout: true,
                            tabSize: 4,
                            insertSpaces: true,
                            lineNumbers: 'on',
                            padding: { top: 16, bottom: 16 },
                            wordWrap: 'on',
                            bracketPairColorization: { enabled: true },
                            autoClosingBrackets: 'always',
                            autoClosingQuotes: 'always',
                            cursorBlinking: 'smooth',
                        }}
                    />
                </section>

                {/* Drag divider */}
                <div
                    onMouseDown={onDragStart}
                    style={{ width: '4px', flexShrink: 0, backgroundColor: isDrag ? C.secondary : C.border, cursor: 'col-resize', transition: 'background-color 0.2s' }}
                    onMouseEnter={e => { if (!isDrag) e.currentTarget.style.backgroundColor = C.secondary; }}
                    onMouseLeave={e => { if (!isDrag) e.currentTarget.style.backgroundColor = C.border; }}
                />

                {/* Right: Real terminal */}
                <section style={{ flex: 1, display: 'flex', flexDirection: 'column', backgroundColor: C.surfaceMin, minWidth: 0 }}>
                    {/* Terminal header */}
                    <div style={{ height: '32px', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 16px', backgroundColor: C.surfaceMin, borderBottom: `1px solid ${C.border}` }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span className="material-symbols-outlined" style={{ fontSize: '14px', color: C.outline }}>terminal</span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase' }}>
                                Terminal
                            </span>
                            {running && (
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.success, marginLeft: '8px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                                    <span style={{ width: '6px', height: '6px', backgroundColor: C.success, borderRadius: '50%', animation: 'pulse 1.5s infinite' }} />
                                    LIVE
                                </span>
                            )}
                        </div>
                        <button
                            onClick={() => termObjRef.current?.clear()}
                            title="Clear terminal"
                            style={{ background: 'none', border: 'none', color: C.outline, cursor: 'pointer', padding: '4px', display: 'flex', alignItems: 'center' }}
                            onMouseEnter={e => e.currentTarget.style.color = C.secondary}
                            onMouseLeave={e => e.currentTarget.style.color = C.outline}
                        >
                            <span className="material-symbols-outlined" style={{ fontSize: '16px' }}>clear_all</span>
                        </button>
                    </div>

                    {/* xterm container */}
                    <div
                        ref={termRef}
                        style={{ flex: 1, padding: '8px 12px', overflow: 'hidden', backgroundColor: C.surfaceMin }}
                    />
                </section>
            </main>

            <style>{`
                @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
                .xterm-viewport::-webkit-scrollbar { width: 8px; }
                .xterm-viewport::-webkit-scrollbar-track { background: ${C.surfaceMin}; }
                .xterm-viewport::-webkit-scrollbar-thumb { background: ${C.border}; }
            `}</style>
        </div>
    );
};

export default CoderCompiler;
