const C = {
    bg: '#131313',
    surface: '#1c1b1b',
    primary: '#f1bc8b',
};

const SHIMMER = {
    background: 'linear-gradient(90deg, #1c1b1b 25%, #2a2a2a 50%, #1c1b1b 75%)',
    backgroundSize: '600px 100%',
    animation: 'sk-shimmer 1.4s infinite linear',
    borderRadius: 2,
};

const shimmerKeyframes = `
@keyframes sk-shimmer {
    0%   { background-position: -600px 0; }
    100% { background-position: 600px 0; }
}
`;

export default function SkeletonLoader({ rows = 3, fullScreen = false, compact = false }) {
    return (
        <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '1.25rem',
            padding: fullScreen ? '3rem 4rem' : compact ? '2rem' : '3rem 2.5rem',
            minHeight: fullScreen ? '100vh' : compact ? undefined : '60vh',
            backgroundColor: C.bg,
            justifyContent: fullScreen ? 'center' : undefined,
        }}>
            <style>{shimmerKeyframes}</style>
            <div style={{ ...SHIMMER, height: 24, width: '30%' }} />
            {Array.from({ length: rows }, (_, i) => (
                <div key={i} style={{ display: 'flex', gap: '0.75rem' }}>
                    <div style={{ ...SHIMMER, height: 40, flex: 1 }} />
                    <div style={{ ...SHIMMER, height: 40, width: 80 }} />
                    <div style={{ ...SHIMMER, height: 40, width: 64 }} />
                </div>
            ))}
        </div>
    );
}
