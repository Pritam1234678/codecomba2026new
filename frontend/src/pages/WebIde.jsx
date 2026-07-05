import { useNavigate } from 'react-router-dom';

// WebIde — redirects to the list page which shows Coming Soon
const WebIde = () => {
    const navigate = useNavigate();
    // Immediately redirect to the list (which shows Coming Soon)
    // This handles any direct /web-contest/:id/ide link attempts
    return (
        <div style={{
            display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center',
            height: '100vh', background: '#131313', color: '#e5e2e1',
            fontFamily: "'JetBrains Mono', monospace",
        }}>
            <span className="material-symbols-outlined" style={{
                fontSize: '64px', color: '#f1bc8b', marginBottom: '20px',
            }}>
                public
            </span>
            <span style={{ fontSize: '11px', letterSpacing: '0.25em', color: '#e9c176', textTransform: 'uppercase', marginBottom: '12px' }}>
                Coming Soon
            </span>
            <h2 style={{ fontFamily: "'Playfair Display', serif", fontSize: '32px', color: '#f1bc8b', margin: '0 0 16px' }}>
                Into the Web
            </h2>
            <p style={{ color: '#d4c4b7', maxWidth: '420px', textAlign: 'center', lineHeight: 1.6, marginBottom: '32px' }}>
                This feature is under active development and will be available soon.
                Full VS Code in browser with real integration testing.
            </p>
            <button
                onClick={() => navigate('/web-contest')}
                style={{
                    padding: '10px 28px', border: '1px solid #50453b',
                    background: 'transparent', color: '#d4c4b7', cursor: 'pointer',
                    fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em',
                }}
            >
                ← Back
            </button>
            <style>{`.material-symbols-outlined{font-variation-settings:'FILL' 0,'wght' 300}`}</style>
        </div>
    );
};

export default WebIde;
