import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import axios from 'axios';
import Editor from '@monaco-editor/react';
import ProblemService from '../services/problem.service';
import api from '../services/api';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';

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
};

const LANGS = ['JAVA', 'CPP', 'PYTHON', 'JAVASCRIPT', 'C'];
const LANG_LABELS = { JAVA: 'Java', CPP: 'C++', PYTHON: 'Python', JAVASCRIPT: 'JavaScript', C: 'C' };
const LANG_MONACO = { JAVA: 'java', CPP: 'cpp', PYTHON: 'python', JAVASCRIPT: 'javascript', C: 'c' };

const DEFAULT_SNIPPETS = {
    JAVA: `import java.util.*;

// USER_CODE_START
class Solution {
    public int solve(int[] arr) {
        // Write your code here
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] arr, int expected, int tc, boolean hidden) {
        int got = new Solution().solve(arr);
        if (got == expected) System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(arr) + ":expected=" + expected + ":got=" + got);
    }
    public static void main(String[] a) {
        test(new int[]{1,2,3}, 6, 1, false);
        test(new int[]{5}, 5, 2, false);
    }
}`,
    CPP: `#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    int solve(vector<int>& arr) {
        // Write your code here
        return 0;
    }
};
// USER_CODE_END

void test(vector<int> arr, int expected, int tc, bool hidden=false) {
    Solution sol; int got = sol.solve(arr);
    if (got == expected) cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\\n";
    else if (hidden) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else { cout << "TC:" << tc << ":FAIL:input=["; for (size_t i=0;i<arr.size();i++){ if(i) cout<<","; cout<<arr[i]; } cout << "]:expected=" << expected << ":got=" << got << "\\n"; }
}
int main(){ test({1,2,3},6,1); test({5},5,2); return 0; }`,
    PYTHON: `# USER_CODE_START
class Solution:
    def solve(self, arr):
        # Write your code here
        return 0
# USER_CODE_END

def test(arr, expected, tc, hidden=False):
    got = Solution().solve(arr)
    if got == expected: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:input={arr}:expected={expected}:got={got}")

test([1,2,3], 6, 1)
test([5], 5, 2)`,
    JAVASCRIPT: `// USER_CODE_START
function solve(arr) {
    // Write your code here
    return 0;
}
// USER_CODE_END

function test(arr, expected, tc, hidden = false) {
    const got = solve(arr);
    if (got === expected) console.log(\`TC:\${tc}:PASS\` + (hidden ? ':hidden' : ''));
    else if (hidden) console.log(\`TC:\${tc}:FAIL:hidden\`);
    else console.log(\`TC:\${tc}:FAIL:input=[\${arr}]:expected=\${expected}:got=\${got}\`);
}
test([1,2,3], 6, 1);
test([5], 5, 2);`,
    C: `#include <stdio.h>

// USER_CODE_START
int solve(int* arr, int n) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void test(int* arr, int n, int expected, int tc, int hidden) {
    int got = solve(arr, n);
    if (got == expected) { if (hidden) printf("TC:%d:PASS:hidden\\n", tc); else printf("TC:%d:PASS\\n", tc); }
    else if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
    else { printf("TC:%d:FAIL:input=[", tc); for (int i=0;i<n;i++){ if(i) printf(","); printf("%d", arr[i]); } printf("]:expected=%d:got=%d\\n", expected, got); }
}
int main(){ int t1[]={1,2,3}; test(t1,3,6,1,0); int t2[]={5}; test(t2,1,5,2,0); return 0; }`,
};

export default function EditProblem() {
    const { id }     = useParams();
    const navigate   = useNavigate();
    const [loading,  setLoading]  = useState(true);
    const [saving,   setSaving]   = useState(false);
    const [error,    setError]    = useState('');
    const [dirty,    setDirty]    = useState(false);
    const [toast,    setToast]    = useState(null);
    const [contestId, setContestId] = useState(null);
    const [activeTab, setActiveTab] = useState('JAVA');
    const [editorTab, setEditorTab] = useState('harness'); // harness | template

    const [formData, setFormData] = useState({
        title: '', description: '', inputFormat: '', outputFormat: '',
        constraints: '', timeLimit: 2, memoryLimit: 256,
        example1: '', example2: '', example3: '', images: '', active: true, level: 'MEDIUM', topics: ''
    });

    const [snippets, setSnippets] = useState(
        Object.fromEntries(LANGS.map(l => [l, { solutionTemplate: DEFAULT_SNIPPETS[l] }]))
    );

    const [contests, setContests] = useState([]);
    const [allContests, setAllContests] = useState([]);

    useEffect(() => {
        const load = async () => {
            try {
                const token = localStorage.getItem('token');
                const h = { Authorization: `Bearer ${token}` };
                const res = await axios.get(`${API_URL}/problems/${id}`, { headers: h });
                const p = res.data;
                setFormData({ title: p.title || '', description: p.description || '', inputFormat: p.inputFormat || '', outputFormat: p.outputFormat || '', constraints: p.constraints || '', timeLimit: (p.timeLimit > 100 ? Math.round(p.timeLimit / 1000) : p.timeLimit) || 2, memoryLimit: p.memoryLimit || 256, example1: p.example1 || '', example2: p.example2 || '', example3: p.example3 || '', images: p.images || '', active: p.active !== undefined ? p.active : true, level: p.level || 'MEDIUM', topics: p.topics || '' });
                if (p.contestId) setContestId(p.contestId);
                try {
                    const sRes = await ProblemService.getSnippetsAdmin(id);
                    const map = Object.fromEntries(LANGS.map(l => [l, { solutionTemplate: DEFAULT_SNIPPETS[l] }]));
                    sRes.data.forEach(s => { map[s.language] = { solutionTemplate: s.solutionTemplate || DEFAULT_SNIPPETS[s.language] || '' }; });
                    setSnippets(map);
                } catch {}
                try {
                    const cRes = await api.get(`/admin/problems/${id}/contests`);
                    setContests(Array.isArray(cRes.data) ? cRes.data : []);
                } catch {}
                try {
                    const acRes = await api.get('/admin/contests');
                    setAllContests(Array.isArray(acRes.data) ? acRes.data : []);
                } catch {}
            } catch { setError('Failed to load problem'); }
            finally { setLoading(false); }
        };
        load();
    }, [id]);

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(p => ({ ...p, [name]: type === 'checkbox' ? checked : (type === 'number' ? parseInt(value) : value) }));
        setDirty(true);
    };

    const showToast = (msg, type = 'success') => { setToast({ msg, type }); setTimeout(() => setToast(null), 3000); };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError('');
        try {
            const token = localStorage.getItem('token');
            const data = { ...formData, example1: formData.example1?.trim() || null, example2: formData.example2?.trim() || null, example3: formData.example3?.trim() || null, images: formData.images?.trim() || null };
            await axios.put(`${API_URL}/admin/problems/${id}`, data, { headers: { Authorization: `Bearer ${token}` } });
            await ProblemService.saveAllSnippets(id, LANGS.map(l => ({ language: l, solutionTemplate: snippets[l].solutionTemplate })));
            setDirty(false);
            showToast('Problem saved successfully.');
            setTimeout(() => navigate(contestId ? `/admin/contests/${contestId}/problems` : '/admin/problems'), 1000);
        } catch (err) { setError(err.response?.data?.message || 'Failed to update problem'); setSaving(false); }
    };

    if (loading) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', color: C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '13px' }}>Loading...</div>
    );

    return (
        <div style={{ backgroundColor: C.bg, color: C.onBg, fontFamily: "'Geist', sans-serif", height: '100vh', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

            {/* ── Task Header ── */}
            <header style={{ height: '72px', flexShrink: 0, borderBottom: `1px solid ${C.border}`, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 32px', backgroundColor: C.bg, zIndex: 10 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                    <button
                        onClick={() => navigate('/admin/problems')}
                        style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '40px', height: '40px', border: `1px solid transparent`, color: C.outline, background: 'none', cursor: 'pointer', transition: 'all 0.2s' }}
                        onMouseEnter={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.secondary; }}
                        onMouseLeave={e => { e.currentTarget.style.borderColor = 'transparent'; e.currentTarget.style.color = C.outline; }}
                    >
                        <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>arrow_back</span>
                    </button>
                    <div style={{ width: '1px', height: '32px', backgroundColor: C.border }} />
                    <div>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '2px' }}>Editing Problem</span>
                        <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 600, color: C.onBg, maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {formData.title || 'Problem'}
                        </h1>
                    </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                    {dirty && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                            <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.secondary, display: 'inline-block' }} />
                            Unsaved Changes
                        </div>
                    )}
                    <div style={{ width: '1px', height: '32px', backgroundColor: C.border }} />
                    <button
                        form="edit-problem-form"
                        type="submit"
                        disabled={saving}
                        style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', border: `1px solid ${C.primary}`, color: saving ? C.outline : C.primary, backgroundColor: 'transparent', padding: '10px 24px', cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.5 : 1, transition: 'all 0.2s' }}
                        onMouseEnter={e => { if (!saving) { e.currentTarget.style.backgroundColor = C.primary; e.currentTarget.style.color = C.bg; } }}
                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = saving ? C.outline : C.primary; }}
                    >
                        {saving ? 'Saving...' : 'Save Changes'}
                    </button>
                </div>
            </header>

            {/* ── Split Workspace ── */}
            <main style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>

                {/* ── Left: Form ── */}
                <section style={{ width: '42%', borderRight: `1px solid ${C.border}`, overflowY: 'auto', backgroundColor: C.bg, display: 'flex', flexDirection: 'column' }}>
                    {error && (
                        <div style={{ margin: '1rem 2rem 0', padding: '10px 14px', border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`, backgroundColor: 'rgba(255,180,171,0.06)', fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error }}>
                            {error}
                        </div>
                    )}
                    <form id="edit-problem-form" onSubmit={handleSubmit} style={{ padding: '2rem', display: 'flex', flexDirection: 'column', gap: '2rem' }}>

                        {/* Core Metadata */}
                        <div>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', marginBottom: '1.5rem' }}>Core Metadata</div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                                <UField label="Problem Title" name="title" type="text" value={formData.title} onChange={handleChange} required />
                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Time Limit (seconds)</label>
                                        <input type="number" name="timeLimit" value={formData.timeLimit} onChange={handleChange} min="1" max="15" step="1"
                                            style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', padding: '8px 0', outline: 'none', boxSizing: 'border-box' }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary} onBlur={e => e.target.style.borderBottomColor = C.border}
                                        />
                                    </div>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Memory Limit (MB)</label>
                                        <input type="number" name="memoryLimit" value={formData.memoryLimit} onChange={handleChange} min="64" step="64"
                                            style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.secondary, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', padding: '8px 0', outline: 'none', boxSizing: 'border-box' }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary} onBlur={e => e.target.style.borderBottomColor = C.border}
                                        />
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Statement Editor */}
                        <div>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', marginBottom: '1rem' }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>Statement Editor</span>
                                <div style={{ display: 'flex', gap: '8px' }}>
                                    {['format_bold', 'format_italic', 'code', 'functions'].map(icon => (
                                        <button key={icon} type="button" style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.outline, transition: 'color 0.2s', padding: '2px' }}
                                            onMouseEnter={e => e.currentTarget.style.color = C.primary} onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                        >
                                            <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{icon}</span>
                                        </button>
                                    ))}
                                </div>
                            </div>
                            <textarea name="description" value={formData.description} onChange={handleChange} required rows={6}
                                style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'Geist', sans-serif", fontSize: '15px', lineHeight: 1.6, padding: '8px 0', outline: 'none', resize: 'vertical', boxSizing: 'border-box' }}
                                onFocus={e => e.target.style.borderBottomColor = C.secondary} onBlur={e => e.target.style.borderBottomColor = C.border}
                            />
                        </div>

                        {/* I/O + Constraints */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            <UField label="Input Format" name="inputFormat" type="textarea" value={formData.inputFormat} onChange={handleChange} />
                            <UField label="Output Format" name="outputFormat" type="textarea" value={formData.outputFormat} onChange={handleChange} />
                            <UField label="Constraints" name="constraints" type="textarea" value={formData.constraints} onChange={handleChange} codeFont />
                        </div>

                        {/* Examples */}
                        <div>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', marginBottom: '1.5rem' }}>Optional Examples</div>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                                <UField label="Example 1" name="example1" type="textarea" value={formData.example1} onChange={handleChange} codeFont />
                                <UField label="Example 2" name="example2" type="textarea" value={formData.example2} onChange={handleChange} codeFont />
                                <UField label="Example 3" name="example3" type="textarea" value={formData.example3} onChange={handleChange} codeFont />
                                <UField label="Images (comma-separated URLs)" name="images" type="text" value={formData.images} onChange={handleChange} />
                            </div>
                        </div>

                        {/* Difficulty Level */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                                Difficulty Level
                            </label>
                            <div style={{ display: 'flex', gap: '8px' }}>
                                {['EASY', 'MEDIUM', 'HARD'].map(lv => {
                                    const colors = { EASY: { c:'#66bb6a', b:'#66bb6a', bg:'rgba(102,187,106,0.12)' }, MEDIUM: { c:'#e9c176', b:'#e9c176', bg:'rgba(233,193,118,0.12)' }, HARD: { c:'#ffb4ab', b:'#ffb4ab', bg:'rgba(255,180,171,0.12)' } };
                                    const lc = colors[lv];
                                    const sel = formData.level === lv;
                                    return (
                                        <button key={lv} type="button"
                                            onClick={() => { setFormData(p => ({ ...p, level: lv })); setDirty(true); }}
                                            style={{ flex: 1, padding: '10px', border: `1px solid ${sel ? lc.b : C.border}`, backgroundColor: sel ? lc.bg : 'transparent', color: sel ? lc.c : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.12em', textTransform: 'uppercase', cursor: 'pointer', transition: 'all 0.2s' }}
                                        >{lv}</button>
                                    );
                                })}
                            </div>
                        </div>

                        {/* Topics */}
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase' }}>
                                Topics
                            </label>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                                {(function() {
                                    const ALL_TOPICS = ['Array', 'String', 'Two Pointers', 'Sliding Window', 'Binary Search', 'Hash Table', 'Linked List', 'Stack', 'Queue', 'Tree', 'Binary Tree', 'BST', 'Heap', 'Graph', 'Dynamic Programming', 'Greedy', 'Sorting', 'Bit Manipulation', 'Math', 'Recursion', 'Backtracking', 'DFS', 'BFS', 'Union Find', 'Trie', 'Divide and Conquer', 'Simulation'];
                                    const selected = formData.topics ? formData.topics.split(',').map(t => t.trim()) : [];
                                    const toggle = (topic) => {
                                        const next = selected.includes(topic)
                                            ? selected.filter(t => t !== topic)
                                            : [...selected, topic];
                                        setFormData(p => ({ ...p, topics: next.join(', ') }));
                                        setDirty(true);
                                    };
                                    return ALL_TOPICS.map(topic => {
                                        const on = selected.includes(topic);
                                        return (
                                            <button key={topic} type="button"
                                                onClick={() => toggle(topic)}
                                                style={{
                                                    padding: '5px 10px', border: `1px solid ${on ? C.secondary : C.border}`,
                                                    backgroundColor: on ? 'rgba(233,193,118,0.12)' : 'transparent',
                                                    color: on ? C.secondary : C.outline,
                                                    fontFamily: "'JetBrains Mono', monospace", fontSize: '9px',
                                                    letterSpacing: '0.06em', cursor: 'pointer',
                                                    transition: 'all 0.15s',
                                                }}
                                            >
                                                {topic}
                                            </button>
                                        );
                                    });
                                })()}
                            </div>
                        </div>

                        {/* Active */}
                        <label style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}>
                            <input type="checkbox" name="active" checked={formData.active} onChange={handleChange} style={{ accentColor: C.primary, width: '16px', height: '16px' }} />
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.muted, letterSpacing: '0.08em', textTransform: 'uppercase' }}>Active (visible to users)</span>
                        </label>

                        {/* Contest Associations */}
                        <div>
                            <div style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.2em', color: C.outline, textTransform: 'uppercase', borderBottom: `1px solid ${C.border}`, paddingBottom: '8px', marginBottom: '1.5rem' }}>Contest Associations</div>
                            {contests.length === 0 ? (
                                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline, letterSpacing: '0.05em', lineHeight: 1.6 }}>
                                    No contests attached. Use the picker below to attach this problem to a contest.
                                </p>
                            ) : (
                                <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    {contests.map(c => (
                                        <li key={c.id}
                                            style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 12px', border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow }}
                                        >
                                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.onBg, letterSpacing: '0.05em', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                                <span style={{ color: C.secondary }}>{c.name || 'Untitled'}</span>
                                                <span style={{ color: C.outline }}>{` (CC-${String(c.id).padStart(4, '0')})`}</span>
                                            </span>
                                            <button
                                                type="button"
                                                onClick={async () => {
                                                    if (!window.confirm(`Detach "${c.name}" from this problem? The contest will lose this problem from its roster.`)) return;
                                                    try {
                                                        await api.delete(`/admin/contests/${c.id}/problems/${id}`);
                                                        setContests(prev => prev.filter(x => x.id !== c.id));
                                                        showToast(`Detached from ${c.name}.`);
                                                    } catch {
                                                        showToast('Failed to detach from contest.', 'error');
                                                    }
                                                }}
                                                style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.12em', textTransform: 'uppercase', border: `1px solid ${C.border}`, color: C.outline, backgroundColor: 'transparent', padding: '6px 14px', cursor: 'pointer', transition: 'all 0.2s' }}
                                                onMouseEnter={e => { e.currentTarget.style.borderColor = C.error; e.currentTarget.style.color = C.error; }}
                                                onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.outline; }}
                                            >
                                                Detach
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                            {/* Attach to Contest picker */}
                            {(() => {
                                const available = allContests.filter(c => !contests.some(x => x.id === c.id));
                                return (
                                    <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                        <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase' }}>Add to Contest</label>
                                        <select
                                            value=""
                                            disabled={available.length === 0}
                                            onChange={async (e) => {
                                                const cid = e.target.value;
                                                if (!cid) return;
                                                const target = allContests.find(c => String(c.id) === String(cid));
                                                const name = target?.name || `CC-${String(cid).padStart(4, '0')}`;
                                                try {
                                                    await api.post(`/admin/contests/${cid}/problems/${id}`);
                                                    if (target) setContests(prev => [...prev, target]);
                                                    e.target.value = '';
                                                    showToast(`Attached to ${name}.`);
                                                } catch {
                                                    e.target.value = '';
                                                    showToast('Failed to attach.', 'error');
                                                }
                                            }}
                                            style={{ width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${C.border}`, color: C.onBg, fontFamily: "'JetBrains Mono', monospace", fontSize: '14px', padding: '8px 0', outline: 'none', boxSizing: 'border-box', cursor: available.length === 0 ? 'not-allowed' : 'pointer', appearance: 'none' }}
                                            onFocus={e => e.target.style.borderBottomColor = C.secondary}
                                            onBlur={e => e.target.style.borderBottomColor = C.border}
                                        >
                                            <option value="" style={{ backgroundColor: C.surfaceLow, color: C.outline }}>
                                                {available.length === 0 ? 'No contests available' : 'Add to Contest...'}
                                            </option>
                                            {available.map(c => (
                                                <option key={c.id} value={c.id} style={{ backgroundColor: C.surfaceLow, color: C.onBg }}>
                                                    {(c.name || 'Untitled') + ` (CC-${String(c.id).padStart(4, '0')})`}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                );
                            })()}
                        </div>
                    </form>
                </section>

                {/* ── Right: Code Editor ── */}
                <section style={{ flex: 1, display: 'flex', flexDirection: 'column', backgroundColor: C.surfaceMin }}>
                    {/* Editor Tabs */}
                    <div style={{ display: 'flex', borderBottom: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, flexShrink: 0, paddingTop: '8px', paddingLeft: '8px', gap: '4px' }}>
                        {[
                            { key: 'harness', label: 'Validator / Harness' },
                        ].map(t => (
                            <button key={t.key} type="button" onClick={() => setEditorTab(t.key)}
                                style={{ padding: '10px 24px', borderBottom: editorTab === t.key ? `2px solid ${C.primary}` : '2px solid transparent', color: editorTab === t.key ? C.primary : C.outline, fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase', background: editorTab === t.key ? C.surfaceHi : 'transparent', border: 'none', cursor: 'pointer', transition: 'all 0.2s' }}
                            >{t.label}</button>
                        ))}
                        <div style={{ flex: 1 }} />
                    </div>

                    {/* Lang Tabs + Toolbar */}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px', borderBottom: `1px solid ${C.border}`, flexShrink: 0, backgroundColor: C.surfaceMin }}>
                        <div style={{ display: 'flex', gap: '1px', backgroundColor: C.border }}>
                            {LANGS.map(l => (
                                <button key={l} type="button" onClick={() => setActiveTab(l)}
                                    style={{ padding: '6px 16px', fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.08em', textTransform: 'uppercase', border: 'none', cursor: 'pointer', backgroundColor: activeTab === l ? C.secondary : C.surfaceMin, color: activeTab === l ? C.bg : C.outline, transition: 'all 0.2s' }}
                                >{LANG_LABELS[l]}</button>
                            ))}
                        </div>
                        <div style={{ display: 'flex', gap: '12px' }}>
                            {['settings', 'subject'].map(icon => (
                                <button key={icon} type="button" style={{ background: 'none', border: 'none', cursor: 'pointer', color: C.outline, transition: 'color 0.2s' }}
                                    onMouseEnter={e => e.currentTarget.style.color = C.onBg} onMouseLeave={e => e.currentTarget.style.color = C.outline}
                                >
                                    <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>{icon}</span>
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Monaco */}
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
                        <div style={{ padding: '6px 16px', backgroundColor: C.surfaceMin, borderBottom: `1px solid ${C.border}`, display: 'flex', justifyContent: 'space-between', flexShrink: 0 }}>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', color: C.outline }}>
                                {LANG_LABELS[activeTab]} — Solution Harness
                            </span>
                            <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.border }}>
                                Mark editable zone with USER_CODE_START / USER_CODE_END
                            </span>
                        </div>
                        <div style={{ flex: 1 }}>
                            <Editor
                                height="100%"
                                theme="vs-dark"
                                language={LANG_MONACO[activeTab]}
                                value={snippets[activeTab].solutionTemplate}
                                onChange={v => { setSnippets(p => ({ ...p, [activeTab]: { solutionTemplate: v || '' } })); setDirty(true); }}
                                options={{ fontSize: 13, fontFamily: "'Fira Code', 'Cascadia Code', monospace", fontLigatures: true, minimap: { enabled: false }, scrollBeyondLastLine: false, automaticLayout: true, lineNumbers: 'on', folding: true, bracketPairColorization: { enabled: true }, autoClosingBrackets: 'always', autoClosingQuotes: 'always', tabSize: 4, insertSpaces: true, wordWrap: 'off', padding: { top: 12, bottom: 12 } }}
                            />
                        </div>
                        {/* Status bar */}
                        <div style={{ height: '28px', borderTop: `1px solid ${C.border}`, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 16px', backgroundColor: C.surfaceLow, fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                            <span>UTF-8</span>
                            <span>{LANG_LABELS[activeTab]}</span>
                        </div>
                    </div>
                </section>
            </main>

            {/* ── Toast ── */}
            <AnimatePresence>
                {toast && (
                    <motion.div initial={{ opacity: 0, x: 40 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: 40 }}
                        style={{ position: 'fixed', bottom: '2rem', right: '2rem', backgroundColor: C.surfaceLow, borderLeft: `2px solid ${toast.type === 'success' ? C.secondary : C.error}`, padding: '1rem 1.5rem', zIndex: 100, fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: toast.type === 'success' ? C.secondary : C.error, letterSpacing: '0.05em' }}
                    >{toast.msg}</motion.div>
                )}
            </AnimatePresence>

            <style>{`.material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }`}</style>
        </div>
    );
}

const UField = ({ label, name, type, value, onChange, placeholder, required, codeFont }) => {
    const [f, setF] = useState(false);
    const isTA = type === 'textarea';
    const style = { width: '100%', backgroundColor: 'transparent', border: 'none', borderBottom: `1px solid ${f ? '#e9c176' : '#50453b'}`, color: '#e5e2e1', fontFamily: codeFont ? "'JetBrains Mono', monospace" : "'Geist', sans-serif", fontSize: '14px', padding: '8px 0', outline: 'none', transition: 'border-color 0.2s', boxSizing: 'border-box', resize: isTA ? 'vertical' : undefined };
    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: '#9d8e83', textTransform: 'uppercase' }}>{label}</label>
            {isTA ? <textarea name={name} value={value} onChange={onChange} rows={3} style={style} onFocus={() => setF(true)} onBlur={() => setF(false)} /> : <input type={type} name={name} value={value} onChange={onChange} placeholder={placeholder} required={required} style={style} onFocus={() => setF(true)} onBlur={() => setF(false)} />}
        </div>
    );
};
