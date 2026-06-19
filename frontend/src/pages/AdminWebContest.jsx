import { useState, useEffect, useRef, useCallback } from 'react';
import Editor from '@monaco-editor/react';
import api from '../services/api';

// ── VS Code-style color tokens ────────────────────────────────────────────────
const C = {
    bg: '#1e1e1e',
    sidebar: '#252526',
    tabBar: '#2d2d2d',
    activeTab: '#1e1e1e',
    inactiveTab: '#2d2d2d',
    border: '#444',
    text: '#cccccc',
    textMuted: '#888',
    accent: '#007acc',
    success: '#4ec9b0',
    warning: '#ce9178',
    error: '#f48771',
    highlight: '#37373d',
    hoverBg: '#2a2d2e',
    inputBg: '#3c3c3c',
};

// ── File icon by extension ────────────────────────────────────────────────────
const fileIcon = (name) => {
    if (!name) return '📄';
    const ext = name.split('.').pop().toLowerCase();
    const icons = {
        java: '☕', py: '🐍', js: '🟨', ts: '🔷',
        json: '📦', xml: '🔧', yml: '⚙️', yaml: '⚙️',
        md: '📝', txt: '📄', sh: '🔩', properties: '⚙️',
        html: '🌐', css: '🎨', sql: '🗄️',
    };
    return icons[ext] || '📄';
};

// ── Monaco language by extension ─────────────────────────────────────────────
const monacoLang = (name) => {
    if (!name) return 'plaintext';
    const ext = name.split('.').pop().toLowerCase();
    const langs = {
        java: 'java', py: 'python', js: 'javascript', ts: 'typescript',
        json: 'json', xml: 'xml', yml: 'yaml', yaml: 'yaml',
        md: 'markdown', sh: 'shell', properties: 'ini', html: 'html',
        css: 'css', sql: 'sql',
    };
    return langs[ext] || 'plaintext';
};

// ── Build file tree from flat path list ──────────────────────────────────────
const buildTree = (paths) => {
    const root = {};
    for (const p of paths) {
        const parts = p.split('/');
        let node = root;
        for (let i = 0; i < parts.length - 1; i++) {
            if (!node[parts[i]]) node[parts[i]] = {};
            node = node[parts[i]];
        }
        const file = parts[parts.length - 1];
        node[file] = null; // null means it's a file
    }
    return root;
};

// ── AdminWebContest main component ────────────────────────────────────────────
const AdminWebContest = () => {
    const [templates, setTemplates] = useState([]);
    const [selectedTemplateId, setSelectedTemplateId] = useState(null);
    const [files, setFiles] = useState({}); // path → content
    const [openTabs, setOpenTabs] = useState([]); // list of paths
    const [activeTab, setActiveTab] = useState(null);
    const [modified, setModified] = useState({}); // path → bool
    const [expandedDirs, setExpandedDirs] = useState({});
    const [statusMsg, setStatusMsg] = useState('Ready');
    const [cursorInfo, setCursorInfo] = useState({ line: 1, col: 1 });
    const [showNewTemplate, setShowNewTemplate] = useState(false);
    const [showManifest, setShowManifest] = useState(false);
    const [manifest, setManifest] = useState(null);
    const [loading, setLoading] = useState(false);
    const [contextMenu, setContextMenu] = useState(null); // { x, y, path }
    const [showNewFile, setShowNewFile] = useState(false);
    const [newFilePath, setNewFilePath] = useState('');
    const [newFileParent, setNewFileParent] = useState('');
    const editorRef = useRef(null);

    // Load template list on mount
    useEffect(() => {
        api.get('/web-contest/admin/templates')
            .then(r => {
                setTemplates(r.data);
                if (r.data.length > 0) setSelectedTemplateId(r.data[0].templateId);
            })
            .catch(err => setStatusMsg('Failed to load templates: ' + (err.response?.data?.message || err.message)));
    }, []);

    // Load files when template changes
    useEffect(() => {
        if (!selectedTemplateId) return;
        setLoading(true);
        setFiles({});
        setOpenTabs([]);
        setActiveTab(null);
        setModified({});
        setStatusMsg('Loading files...');

        api.get(`/web-contest/admin/templates/${selectedTemplateId}/files`)
            .then(r => {
                setFiles(r.data);
                setStatusMsg('Files loaded');
            })
            .catch(err => setStatusMsg('Error loading files: ' + (err.response?.data?.message || err.message)))
            .finally(() => setLoading(false));

        api.get(`/web-contest/admin/templates/${selectedTemplateId}/manifest`)
            .then(r => setManifest(r.data))
            .catch(() => setManifest(null));
    }, [selectedTemplateId]);

    const openFile = useCallback((path) => {
        if (!openTabs.includes(path)) {
            setOpenTabs(t => [...t, path]);
        }
        setActiveTab(path);
    }, [openTabs]);

    const closeTab = useCallback((path, e) => {
        e.stopPropagation();
        setOpenTabs(tabs => {
            const idx = tabs.indexOf(path);
            const next = tabs.filter(t => t !== path);
            if (activeTab === path) {
                setActiveTab(next[Math.min(idx, next.length - 1)] || null);
            }
            return next;
        });
        setModified(m => { const n = { ...m }; delete n[path]; return n; });
    }, [activeTab]);

    const handleEditorChange = useCallback((value) => {
        if (!activeTab) return;
        setFiles(f => ({ ...f, [activeTab]: value }));
        setModified(m => ({ ...m, [activeTab]: true }));
    }, [activeTab]);

    const saveFile = useCallback(async (path) => {
        if (!selectedTemplateId || !path) return;
        setStatusMsg('Saving...');
        try {
            await api.post(`/web-contest/admin/templates/${selectedTemplateId}/files`, {
                path,
                content: files[path] || '',
            });
            setModified(m => ({ ...m, [path]: false }));
            setStatusMsg(`Saved: ${path}`);
        } catch (err) {
            setStatusMsg('Save failed: ' + (err.response?.data?.message || err.message));
        }
    }, [selectedTemplateId, files]);

    const saveAll = useCallback(async () => {
        const unsaved = Object.keys(modified).filter(p => modified[p]);
        if (!unsaved.length) { setStatusMsg('Nothing to save'); return; }
        setStatusMsg(`Saving ${unsaved.length} files...`);
        for (const p of unsaved) {
            await saveFile(p);
        }
        setStatusMsg('All saved');
    }, [modified, saveFile]);

    // Ctrl+S handler
    useEffect(() => {
        const handler = (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
                e.preventDefault();
                if (activeTab) saveFile(activeTab);
            }
        };
        window.addEventListener('keydown', handler);
        return () => window.removeEventListener('keydown', handler);
    }, [activeTab, saveFile]);

    const deleteFile = useCallback(async (path) => {
        if (!selectedTemplateId || !path) return;
        if (!window.confirm(`Delete ${path}?`)) return;
        try {
            await api.delete(`/web-contest/admin/templates/${selectedTemplateId}/files`, { params: { path } });
            setFiles(f => { const n = { ...f }; delete n[path]; return n; });
            setOpenTabs(t => t.filter(x => x !== path));
            if (activeTab === path) setActiveTab(null);
            setStatusMsg(`Deleted: ${path}`);
        } catch (err) {
            setStatusMsg('Delete failed: ' + (err.response?.data?.message || err.message));
        }
    }, [selectedTemplateId, activeTab]);

    const createNewFile = useCallback(async () => {
        const fullPath = newFileParent ? `${newFileParent}/${newFilePath}` : newFilePath;
        if (!fullPath.trim()) return;
        setFiles(f => ({ ...f, [fullPath]: '' }));
        setModified(m => ({ ...m, [fullPath]: true }));
        openFile(fullPath);
        setShowNewFile(false);
        setNewFilePath('');
        setNewFileParent('');
    }, [newFilePath, newFileParent, openFile]);

    const saveManifest = useCallback(async () => {
        if (!selectedTemplateId || !manifest) return;
        try {
            await api.put(`/web-contest/admin/templates/${selectedTemplateId}/manifest`, manifest);
            setStatusMsg('Manifest saved');
            setShowManifest(false);
        } catch (err) {
            setStatusMsg('Manifest save failed: ' + (err.response?.data?.message || err.message));
        }
    }, [selectedTemplateId, manifest]);

    const toggleDir = (path) => {
        setExpandedDirs(e => ({ ...e, [path]: !e[path] }));
    };

    const handleContextMenu = (e, path) => {
        e.preventDefault();
        setContextMenu({ x: e.clientX, y: e.clientY, path });
    };

    const closeContextMenu = () => setContextMenu(null);

    const currentTemplate = templates.find(t => t.templateId === selectedTemplateId);
    const tree = buildTree(Object.keys(files));

    return (
        <div
            style={{ display: 'flex', flexDirection: 'column', height: '100vh', backgroundColor: C.bg, color: C.text, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', overflow: 'hidden' }}
            onClick={closeContextMenu}
        >
            {/* ── Top bar ── */}
            <TopBar
                templates={templates}
                selectedTemplateId={selectedTemplateId}
                onSelectTemplate={setSelectedTemplateId}
                onNewTemplate={() => setShowNewTemplate(true)}
                onSaveAll={saveAll}
                onManifest={() => setShowManifest(true)}
            />

            {/* ── Main area ── */}
            <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
                {/* ── File explorer sidebar ── */}
                <div style={{ width: '250px', flexShrink: 0, backgroundColor: C.sidebar, borderRight: `1px solid ${C.border}`, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                    <div style={{ padding: '8px 12px', fontSize: '11px', letterSpacing: '0.1em', color: C.textMuted, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>EXPLORER</span>
                        <button
                            onClick={() => { setNewFileParent(''); setNewFilePath(''); setShowNewFile(true); }}
                            title="New File"
                            style={{ background: 'none', border: 'none', color: C.textMuted, cursor: 'pointer', fontSize: '16px', padding: '0 2px', lineHeight: 1 }}
                        >
                            +
                        </button>
                    </div>

                    <div style={{ overflowY: 'auto', flex: 1 }}>
                        {loading ? (
                            <div style={{ padding: '16px', color: C.textMuted }}>Loading...</div>
                        ) : Object.keys(tree).length === 0 ? (
                            <div style={{ padding: '16px', color: C.textMuted }}>No files yet</div>
                        ) : (
                            <FileTree
                                node={tree}
                                prefix=""
                                depth={0}
                                expandedDirs={expandedDirs}
                                toggleDir={toggleDir}
                                activeFile={activeTab}
                                onOpenFile={openFile}
                                onContextMenu={handleContextMenu}
                            />
                        )}
                    </div>
                </div>

                {/* ── Editor panel ── */}
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                    {/* Tab bar */}
                    <div style={{ backgroundColor: C.tabBar, display: 'flex', overflowX: 'auto', borderBottom: `1px solid ${C.border}`, flexShrink: 0 }}>
                        {openTabs.map(tab => (
                            <EditorTab
                                key={tab}
                                path={tab}
                                active={tab === activeTab}
                                modified={!!modified[tab]}
                                onClick={() => setActiveTab(tab)}
                                onClose={(e) => closeTab(tab, e)}
                            />
                        ))}
                    </div>

                    {/* Monaco editor */}
                    <div style={{ flex: 1, overflow: 'hidden' }}>
                        {activeTab ? (
                            <Editor
                                height="100%"
                                language={monacoLang(activeTab)}
                                value={files[activeTab] || ''}
                                theme="vs-dark"
                                onChange={handleEditorChange}
                                onMount={(editor) => {
                                    editorRef.current = editor;
                                    editor.onDidChangeCursorPosition(e => {
                                        setCursorInfo({ line: e.position.lineNumber, col: e.position.column });
                                    });
                                }}
                                options={{
                                    fontSize: 13,
                                    fontFamily: "'JetBrains Mono', monospace",
                                    minimap: { enabled: false },
                                    scrollBeyondLastLine: false,
                                    wordWrap: 'off',
                                    automaticLayout: true,
                                    tabSize: 4,
                                    insertSpaces: true,
                                    lineNumbers: 'on',
                                    renderLineHighlight: 'line',
                                    smoothScrolling: true,
                                }}
                            />
                        ) : (
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: C.textMuted, flexDirection: 'column', gap: '12px' }}>
                                <span style={{ fontSize: '48px' }}>📁</span>
                                <span>Select a file to edit</span>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* ── Status bar ── */}
            <StatusBar
                filePath={activeTab}
                lang={activeTab ? monacoLang(activeTab) : ''}
                cursor={cursorInfo}
                message={statusMsg}
            />

            {/* ── Context menu ── */}
            {contextMenu && (
                <ContextMenu
                    x={contextMenu.x}
                    y={contextMenu.y}
                    path={contextMenu.path}
                    onDelete={() => { deleteFile(contextMenu.path); closeContextMenu(); }}
                    onNewFile={() => {
                        const dir = contextMenu.path.includes('/') ? contextMenu.path.split('/').slice(0, -1).join('/') : '';
                        setNewFileParent(dir);
                        setNewFilePath('');
                        setShowNewFile(true);
                        closeContextMenu();
                    }}
                    onClose={closeContextMenu}
                />
            )}

            {/* ── New file modal ── */}
            {showNewFile && (
                <Modal title="New File" onClose={() => setShowNewFile(false)}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                        {newFileParent && <div style={{ color: C.textMuted, fontSize: '12px' }}>Parent: {newFileParent}/</div>}
                        <input
                            autoFocus
                            value={newFilePath}
                            onChange={e => setNewFilePath(e.target.value)}
                            onKeyDown={e => e.key === 'Enter' && createNewFile()}
                            placeholder="filename.java"
                            style={{ padding: '8px', backgroundColor: C.inputBg, border: `1px solid ${C.border}`, color: C.text, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', outline: 'none' }}
                        />
                        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                            <ModalBtn label="Cancel" onClick={() => setShowNewFile(false)} />
                            <ModalBtn label="Create" accent onClick={createNewFile} />
                        </div>
                    </div>
                </Modal>
            )}

            {/* ── New template modal ── */}
            {showNewTemplate && (
                <NewTemplateModal
                    onClose={() => setShowNewTemplate(false)}
                    onCreate={async (data) => {
                        try {
                            await api.post('/web-contest/admin/templates', data);
                            const r = await api.get('/web-contest/admin/templates');
                            setTemplates(r.data);
                            setShowNewTemplate(false);
                            setStatusMsg('Template created');
                        } catch (err) {
                            setStatusMsg('Create failed: ' + (err.response?.data?.message || err.message));
                        }
                    }}
                />
            )}

            {/* ── Manifest editor modal ── */}
            {showManifest && manifest && (
                <ManifestModal
                    manifest={manifest}
                    files={Object.keys(files)}
                    onChange={setManifest}
                    onSave={saveManifest}
                    onClose={() => setShowManifest(false)}
                />
            )}
        </div>
    );
};

// ── Top bar ───────────────────────────────────────────────────────────────────
const TopBar = ({ templates, selectedTemplateId, onSelectTemplate, onNewTemplate, onSaveAll, onManifest }) => (
    <div style={{ backgroundColor: C.tabBar, borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', gap: '12px', padding: '6px 12px', flexShrink: 0 }}>
        <span style={{ color: C.accent, fontWeight: 700, fontSize: '14px', marginRight: '8px' }}>⚡ Web IDE</span>

        {/* Template selector */}
        <select
            value={selectedTemplateId || ''}
            onChange={e => onSelectTemplate(Number(e.target.value))}
            style={{ backgroundColor: C.inputBg, border: `1px solid ${C.border}`, color: C.text, padding: '4px 8px', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', outline: 'none', cursor: 'pointer' }}
        >
            {templates.length === 0 && <option value="">No templates</option>}
            {templates.map(t => (
                <option key={t.templateId} value={t.templateId}>
                    {t.title} — {t.language}
                </option>
            ))}
        </select>

        <TopBtn label="+ New Template" onClick={onNewTemplate} />
        <div style={{ flex: 1 }} />
        <TopBtn label="📋 Manifest" onClick={onManifest} />
        <TopBtn label="💾 Save All" onClick={onSaveAll} accent />
    </div>
);

const TopBtn = ({ label, onClick, accent }) => {
    const [hov, setHov] = useState(false);
    return (
        <button
            onClick={onClick}
            onMouseEnter={() => setHov(true)}
            onMouseLeave={() => setHov(false)}
            style={{
                padding: '4px 12px', border: `1px solid ${accent ? C.accent : C.border}`,
                backgroundColor: hov ? (accent ? C.accent : C.highlight) : 'transparent',
                color: accent ? (hov ? '#fff' : C.accent) : C.text,
                fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                cursor: 'pointer', transition: 'all 0.15s',
            }}
        >
            {label}
        </button>
    );
};

// ── Editor tab ────────────────────────────────────────────────────────────────
const EditorTab = ({ path, active, modified, onClick, onClose }) => {
    const name = path.split('/').pop();
    const [hov, setHov] = useState(false);
    return (
        <div
            onClick={onClick}
            onMouseEnter={() => setHov(true)}
            onMouseLeave={() => setHov(false)}
            style={{
                display: 'flex', alignItems: 'center', gap: '6px',
                padding: '6px 14px 6px 10px',
                backgroundColor: active ? C.activeTab : C.inactiveTab,
                borderRight: `1px solid ${C.border}`,
                borderTop: active ? `1px solid ${C.accent}` : '1px solid transparent',
                cursor: 'pointer', flexShrink: 0,
                color: active ? C.text : C.textMuted,
                fontSize: '12px', whiteSpace: 'nowrap',
                transition: 'color 0.1s',
            }}
        >
            <span>{fileIcon(name)}</span>
            <span>{name}</span>
            {modified && <span style={{ color: C.warning, fontSize: '10px' }}>●</span>}
            <button
                onClick={onClose}
                style={{
                    background: 'none', border: 'none', color: hov ? C.text : 'transparent',
                    cursor: 'pointer', fontSize: '14px', padding: '0 2px', lineHeight: 1,
                    transition: 'color 0.1s',
                }}
            >
                ×
            </button>
        </div>
    );
};

// ── Recursive file tree ───────────────────────────────────────────────────────
const FileTree = ({ node, prefix, depth, expandedDirs, toggleDir, activeFile, onOpenFile, onContextMenu }) => {
    const entries = Object.entries(node).sort(([aName, aVal], [bName, bVal]) => {
        // Directories first
        const aIsDir = aVal !== null;
        const bIsDir = bVal !== null;
        if (aIsDir && !bIsDir) return -1;
        if (!aIsDir && bIsDir) return 1;
        return aName.localeCompare(bName);
    });

    return (
        <div>
            {entries.map(([name, val]) => {
                const fullPath = prefix ? `${prefix}/${name}` : name;
                const isDir = val !== null && typeof val === 'object';
                const isExpanded = expandedDirs[fullPath];
                const isActive = fullPath === activeFile;

                return (
                    <div key={fullPath}>
                        <FileTreeItem
                            name={name}
                            fullPath={fullPath}
                            isDir={isDir}
                            isExpanded={isExpanded}
                            isActive={isActive}
                            depth={depth}
                            onToggle={() => toggleDir(fullPath)}
                            onOpen={() => !isDir && onOpenFile(fullPath)}
                            onContextMenu={(e) => onContextMenu(e, fullPath)}
                        />
                        {isDir && isExpanded && (
                            <FileTree
                                node={val}
                                prefix={fullPath}
                                depth={depth + 1}
                                expandedDirs={expandedDirs}
                                toggleDir={toggleDir}
                                activeFile={activeFile}
                                onOpenFile={onOpenFile}
                                onContextMenu={onContextMenu}
                            />
                        )}
                    </div>
                );
            })}
        </div>
    );
};

const FileTreeItem = ({ name, fullPath, isDir, isExpanded, isActive, depth, onToggle, onOpen, onContextMenu }) => {
    const [hov, setHov] = useState(false);
    return (
        <div
            onClick={isDir ? onToggle : onOpen}
            onContextMenu={onContextMenu}
            onMouseEnter={() => setHov(true)}
            onMouseLeave={() => setHov(false)}
            style={{
                display: 'flex', alignItems: 'center', gap: '6px',
                padding: `3px 8px 3px ${16 + depth * 16}px`,
                backgroundColor: isActive ? C.accent + '33' : hov ? C.hoverBg : 'transparent',
                cursor: 'pointer', color: isActive ? C.text : C.textMuted,
                fontSize: '13px', userSelect: 'none',
                transition: 'background-color 0.1s',
            }}
        >
            <span style={{ fontSize: '11px', width: '12px', flexShrink: 0 }}>
                {isDir ? (isExpanded ? '▼' : '▶') : ''}
            </span>
            <span style={{ fontSize: '14px' }}>{isDir ? (isExpanded ? '📂' : '📁') : fileIcon(name)}</span>
            <span>{name}</span>
        </div>
    );
};

// ── Status bar ────────────────────────────────────────────────────────────────
const StatusBar = ({ filePath, lang, cursor, message }) => (
    <div style={{
        backgroundColor: C.accent, color: '#fff',
        display: 'flex', alignItems: 'center', gap: '16px',
        padding: '2px 12px', fontSize: '12px', flexShrink: 0,
    }}>
        <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {message}
        </span>
        {filePath && (
            <span style={{ opacity: 0.85 }}>{filePath}</span>
        )}
        {lang && (
            <span style={{ opacity: 0.85, textTransform: 'capitalize' }}>{lang}</span>
        )}
        <span style={{ opacity: 0.85 }}>Ln {cursor.line}, Col {cursor.col}</span>
    </div>
);

// ── Context menu ──────────────────────────────────────────────────────────────
const ContextMenu = ({ x, y, path, onDelete, onNewFile, onClose }) => (
    <div
        style={{
            position: 'fixed', top: y, left: x, zIndex: 9999,
            backgroundColor: '#252526', border: `1px solid ${C.border}`,
            boxShadow: '0 4px 12px rgba(0,0,0,0.5)',
            minWidth: '180px',
        }}
        onClick={e => e.stopPropagation()}
    >
        <CtxItem label="📄 New File Here" onClick={onNewFile} />
        <div style={{ height: '1px', backgroundColor: C.border, margin: '4px 0' }} />
        <CtxItem label="🗑 Delete" onClick={onDelete} danger />
    </div>
);

const CtxItem = ({ label, onClick, danger }) => {
    const [hov, setHov] = useState(false);
    return (
        <div
            onClick={onClick}
            onMouseEnter={() => setHov(true)}
            onMouseLeave={() => setHov(false)}
            style={{
                padding: '7px 16px', cursor: 'pointer',
                backgroundColor: hov ? C.highlight : 'transparent',
                color: danger ? C.error : C.text, fontSize: '13px',
                transition: 'background-color 0.1s',
            }}
        >
            {label}
        </div>
    );
};

// ── Generic modal ─────────────────────────────────────────────────────────────
const Modal = ({ title, onClose, children }) => (
    <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.6)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ backgroundColor: '#252526', border: `1px solid ${C.border}`, minWidth: '360px', maxWidth: '500px', width: '100%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 16px', borderBottom: `1px solid ${C.border}` }}>
                <span style={{ color: C.text, fontSize: '13px', fontWeight: 600 }}>{title}</span>
                <button onClick={onClose} style={{ background: 'none', border: 'none', color: C.textMuted, cursor: 'pointer', fontSize: '18px', lineHeight: 1 }}>×</button>
            </div>
            <div style={{ padding: '16px' }}>{children}</div>
        </div>
    </div>
);

const ModalBtn = ({ label, onClick, accent }) => {
    const [hov, setHov] = useState(false);
    return (
        <button
            onClick={onClick}
            onMouseEnter={() => setHov(true)}
            onMouseLeave={() => setHov(false)}
            style={{
                padding: '6px 16px', border: `1px solid ${accent ? C.accent : C.border}`,
                backgroundColor: hov ? (accent ? C.accent : C.highlight) : 'transparent',
                color: accent ? (hov ? '#fff' : C.accent) : C.text,
                fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                cursor: 'pointer', transition: 'all 0.15s',
            }}
        >
            {label}
        </button>
    );
};

// ── New template modal ────────────────────────────────────────────────────────
const NewTemplateModal = ({ onClose, onCreate }) => {
    const [problemId, setProblemId] = useState('');
    const [language, setLanguage] = useState('JAVA');
    const [templatePath, setTemplatePath] = useState('');

    return (
        <Modal title="New Template" onClose={onClose}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <label style={{ color: C.textMuted, fontSize: '12px' }}>Problem ID</label>
                <input
                    type="number"
                    value={problemId}
                    onChange={e => setProblemId(e.target.value)}
                    placeholder="e.g. 42"
                    style={{ padding: '8px', backgroundColor: C.inputBg, border: `1px solid ${C.border}`, color: C.text, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', outline: 'none' }}
                />
                <label style={{ color: C.textMuted, fontSize: '12px' }}>Language</label>
                <select
                    value={language}
                    onChange={e => setLanguage(e.target.value)}
                    style={{ padding: '8px', backgroundColor: C.inputBg, border: `1px solid ${C.border}`, color: C.text, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', outline: 'none' }}
                >
                    <option value="JAVA">JAVA</option>
                    <option value="NODEJS">NODEJS</option>
                    <option value="PYTHON">PYTHON</option>
                </select>
                <label style={{ color: C.textMuted, fontSize: '12px' }}>Template directory path (absolute)</label>
                <input
                    value={templatePath}
                    onChange={e => setTemplatePath(e.target.value)}
                    placeholder="/path/to/web-contest-templates/my-template"
                    style={{ padding: '8px', backgroundColor: C.inputBg, border: `1px solid ${C.border}`, color: C.text, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', outline: 'none' }}
                />
                <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', marginTop: '8px' }}>
                    <ModalBtn label="Cancel" onClick={onClose} />
                    <ModalBtn label="Create" accent onClick={() => onCreate({ problemId: Number(problemId), language, templatePath })} />
                </div>
            </div>
        </Modal>
    );
};

// ── Manifest editor modal ─────────────────────────────────────────────────────
const ManifestModal = ({ manifest, files, onChange, onSave, onClose }) => {
    const [tab, setTab] = useState('editable'); // editable | readonly | hidden

    const getList = (key) => manifest[key] || [];

    const toggleFile = (key, path) => {
        const current = getList(key);
        const updated = current.includes(path)
            ? current.filter(p => p !== path)
            : [...current, path];
        onChange({ ...manifest, [key]: updated });
    };

    const tabs = [
        { key: 'editableFiles', label: 'Editable', color: C.success },
        { key: 'readonlyFiles', label: 'Read-Only', color: C.warning },
        { key: 'hiddenFiles', label: 'Hidden', color: C.error },
    ];

    const activeKey = tab === 'editable' ? 'editableFiles' : tab === 'readonly' ? 'readonlyFiles' : 'hiddenFiles';

    return (
        <Modal title="Manifest Editor" onClose={onClose}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <p style={{ color: C.textMuted, fontSize: '12px', margin: 0 }}>
                    Configure which files users can edit, see as read-only, or never see (hidden test files).
                </p>

                {/* Category tabs */}
                <div style={{ display: 'flex', gap: '8px' }}>
                    {tabs.map(t => (
                        <button
                            key={t.key}
                            onClick={() => setTab(t.key.replace('Files', '').toLowerCase())}
                            style={{
                                padding: '4px 12px', border: `1px solid ${t.color}`,
                                backgroundColor: activeKey === t.key ? t.color + '33' : 'transparent',
                                color: t.color, fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '11px', cursor: 'pointer',
                            }}
                        >
                            {t.label} ({getList(t.key).length})
                        </button>
                    ))}
                </div>

                {/* File checkboxes */}
                <div style={{ maxHeight: '240px', overflowY: 'auto', backgroundColor: C.bg, border: `1px solid ${C.border}`, padding: '8px' }}>
                    {files.length === 0 ? (
                        <div style={{ color: C.textMuted, fontSize: '12px' }}>No files loaded</div>
                    ) : files.map(f => (
                        <label key={f} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '4px 0', cursor: 'pointer' }}>
                            <input
                                type="checkbox"
                                checked={getList(activeKey).includes(f)}
                                onChange={() => toggleFile(activeKey, f)}
                                style={{ accentColor: C.accent }}
                            />
                            <span style={{ color: C.text, fontSize: '12px' }}>{f}</span>
                        </label>
                    ))}
                </div>

                <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', marginTop: '4px' }}>
                    <ModalBtn label="Cancel" onClick={onClose} />
                    <ModalBtn label="Save Manifest" accent onClick={onSave} />
                </div>
            </div>
        </Modal>
    );
};

export default AdminWebContest;
