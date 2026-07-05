import { useNavigate } from 'react-router-dom';

const C = {
    bg: '#131313', border: '#50453b', primary: '#f1bc8b',
    secondary: '#e9c176', muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1',
};

const WebContestList = () => {
    const navigate = useNavigate();

    return (
        <div style={{
            backgroundColor: C.bg, color: C.onBg,
            fontFamily: "'Geist', sans-serif",
            minHeight: '100vh',
            display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center',
            padding: '40px 24px', textAlign: 'center',
        }}>
            {/* Globe icon */}
            <span className="material-symbols-outlined" style={{
                fontSize: '72px', color: C.primary, marginBottom: '24px',
                fontVariationSettings: "'FILL' 0, 'wght' 200",
            }}>
                public
            </span>

            {/* Label */}
            <span style={{
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '11px', letterSpacing: '0.25em',
                color: C.secondary, textTransform: 'uppercase', marginBottom: '16px',
            }}>
                Coming Soon
            </span>

            {/* Title */}
            <h1 style={{
                fontFamily: "'Playfair Display', serif",
                fontSize: 'clamp(40px, 5vw, 64px)',
                fontWeight: 700, lineHeight: 1.1,
                color: C.primary, margin: '0 0 24px',
            }}>
                Into the Web
            </h1>

            {/* Description */}
            <p style={{
                fontSize: '16px', color: C.muted,
                lineHeight: 1.7, maxWidth: '560px', margin: '0 0 12px',
            }}>
                A new kind of coding challenge — build real-world backend APIs and web
                services inside a <strong style={{ color: C.onBg }}>full VS Code environment</strong>,
                right in your browser.
            </p>
            <p style={{
                fontSize: '15px', color: C.muted,
                lineHeight: 1.7, maxWidth: '500px', margin: '0 0 40px',
            }}>
                Write Spring Boot controllers, FastAPI endpoints, and Express routes.
                Pass real integration tests. Ship code that actually works.
            </p>

            {/* Feature chips */}
            <div style={{
                display: 'flex', flexWrap: 'wrap', gap: '12px',
                justifyContent: 'center', marginBottom: '48px',
            }}>
                {[
                    { icon: 'terminal', text: 'Full VS Code in browser' },
                    { icon: 'science', text: 'JUnit / pytest / Jest tests' },
                    { icon: 'code', text: 'Java · Python · Node.js' },
                    { icon: 'bolt', text: 'Real-time feedback' },
                    { icon: 'emoji_events', text: 'Contest leaderboard' },
                ].map(({ icon, text }) => (
                    <div key={text} style={{
                        display: 'flex', alignItems: 'center', gap: '8px',
                        padding: '8px 16px',
                        border: `1px solid ${C.border}`,
                        color: C.muted,
                        fontFamily: "'JetBrains Mono', monospace", fontSize: '12px',
                    }}>
                        <span className="material-symbols-outlined" style={{ fontSize: '16px', color: C.secondary }}>
                            {icon}
                        </span>
                        {text}
                    </div>
                ))}
            </div>

            {/* Back button */}
            <button
                onClick={() => navigate('/dashboard')}
                style={{
                    padding: '12px 32px',
                    border: `1px solid ${C.border}`,
                    background: 'transparent', color: C.muted,
                    fontFamily: "'JetBrains Mono', monospace",
                    fontSize: '12px', letterSpacing: '0.1em',
                    textTransform: 'uppercase', cursor: 'pointer',
                    transition: 'all 0.2s',
                }}
                onMouseEnter={e => { e.currentTarget.style.borderColor = C.primary; e.currentTarget.style.color = C.primary; }}
                onMouseLeave={e => { e.currentTarget.style.borderColor = C.border; e.currentTarget.style.color = C.muted; }}
            >
                ← Back to Dashboard
            </button>

            <style>{`.material-symbols-outlined{font-variation-settings:'FILL' 0,'wght' 300}`}</style>
        </div>
    );
};

export default WebContestList;
