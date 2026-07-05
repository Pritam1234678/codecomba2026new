import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import AuthService from '../services/auth.service';

const C = {
    bg: '#0e0e0e',
    border: '#50453b',
    primary: '#f1bc8b',
    secondary: '#e9c176',
    muted: '#d4c4b7',
    outline: '#9d8e83',
    onBg: '#e5e2e1',
    surfaceLow: '#1c1b1b',
    surfaceHi: '#2a2a2a',
    error: '#ffb4ab',
};

// ── Admin nav links ───────────────────────────────────────────────────────────
const ADMIN_NAV = [
    { label: 'Dashboard', icon: 'admin_panel_settings', to: '/admin/dashboard' },
    { label: 'Contests', icon: 'military_tech', to: '/admin/contests' },
    { label: 'Problems', icon: 'code', to: '/admin/problems' },
    { label: 'Duel', icon: 'swords', to: '/admin/duels' },
    { label: 'Into the Web', icon: 'public', to: '/admin/web-contest' },
    { label: 'Proctoring', icon: 'shield', to: '/admin/proctoring' },
    { label: 'Users', icon: 'group', to: '/admin/users' },
    { label: 'Rankings', icon: 'leaderboard', to: '/admin/leaderboard' },
    { label: 'Compiler', icon: 'terminal', to: '/compiler' },
    { label: 'Platform', icon: 'tune', to: '/admin/platform-details' },
    { label: 'Support', icon: 'help_outline', to: '/support' },
    { label: 'Mail', icon: 'mail', to: 'https://mail.codecoder.in', external: true },
];

// ── User nav links (mirrors Navbar user links) ────────────────────────────────
const USER_NAV = [
    { label: 'Dashboard', icon: 'dashboard', to: '/dashboard' },
    { label: 'Contests', icon: 'emoji_events', to: '/contests' },
    { label: 'Practice', icon: 'fitness_center', to: '/practice' },
    { label: 'Duel', icon: 'swords', to: '/duel' },
    { label: 'Into the Web', icon: 'public', to: '/web-contest' },
    { label: 'Players', icon: 'group', to: '/players' },
    { label: 'Compiler', icon: 'terminal', to: '/compiler' },
    { label: 'Platform Details', icon: 'settings', to: '/platform-details' },
    { label: 'Support', icon: 'help_outline', to: '/support' },
    { label: 'Edit Profile', icon: 'account_circle', to: '/profile/edit' },
    { label: 'Profile', icon: 'person', to: '/profile' },
];

const AppSidebar = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);

    const currentUser = AuthService.getCurrentUser();
    const isAdmin = currentUser?.roles?.includes('ROLE_ADMIN');
    const navItems = isAdmin ? ADMIN_NAV : USER_NAV;

    const handleLogout = () => {
        AuthService.logout();
        navigate('/');
    };

    return (
        <aside
            style={{
                backgroundColor: C.bg,
                borderRight: `1px solid ${C.border}`,
                width: open ? '240px' : '72px',
                flexShrink: 0,
                display: 'flex',
                flexDirection: 'column',
                padding: '1.5rem 0',
                transition: 'width 0.25s ease',
                overflow: 'hidden',
                position: 'sticky',
                top: 0,
                height: '100vh',
                zIndex: 40,
            }}
            onMouseEnter={() => setOpen(true)}
            onMouseLeave={() => setOpen(false)}
        >
            {/* ── Brand ── */}
            <div style={{
                padding: '0 1.25rem',
                marginBottom: '2.5rem',
                display: 'flex', alignItems: 'center', gap: '12px',
                overflow: 'hidden', whiteSpace: 'nowrap',
            }}>
                {/* Minimised: show "CC" logo; Expanded: show full name */}
                {!open ? (
                    <Link to="/" style={{ textDecoration: 'none', flexShrink: 0 }}>
                        <span style={{
                            fontFamily: "'Playfair Display', serif",
                            fontSize: '22px', fontWeight: 700,
                            color: C.primary, fontStyle: 'italic',
                        }}>
                            CC
                        </span>
                    </Link>
                ) : (
                    <Link to="/" style={{ textDecoration: 'none' }}>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '16px', color: C.primary, fontStyle: 'italic', display: 'block', lineHeight: 1.2 }}>
                            Code Coder
                        </span>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, letterSpacing: '0.12em', textTransform: 'uppercase' }}>
                            {isAdmin ? 'Admin Mode' : 'User Mode'}
                        </span>
                    </Link>
                )}
            </div>

            {/* ── Nav links ── */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', padding: '0 10px', flexGrow: 1 }}>
                {navItems.map(({ label, icon, to, external }) => {
                    const active = !external && (location.pathname === to ||
                        (to !== '/' && location.pathname.startsWith(to) && to.length > 1));
                    return (
                        <SidebarLink key={to} to={to} icon={icon} label={label} active={active} open={open} external={external} />
                    );
                })}
            </div>

            {/* ── User identity + Logout ── */}
            <div style={{ padding: '0 10px', borderTop: `1px solid ${C.border}`, paddingTop: '1rem', marginTop: '0.5rem' }}>
                {/* Username */}
                <div style={{
                    display: 'flex', alignItems: 'center', gap: '12px',
                    padding: '10px 12px', marginBottom: '4px',
                    overflow: 'hidden', whiteSpace: 'nowrap',
                }}>
                    <div style={{
                        width: '32px', height: '32px', borderRadius: '50%',
                        backgroundColor: C.surfaceLow,
                        border: `1px solid ${C.border}`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        flexShrink: 0,
                    }}>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '14px', fontWeight: 700, color: C.primary }}>
                            {currentUser?.username?.charAt(0).toUpperCase() || 'U'}
                        </span>
                    </div>
                    {open && (
                        <div>
                            <p style={{ fontFamily: "'Geist', sans-serif", fontSize: '13px', color: C.onBg, margin: 0, lineHeight: 1.2 }}>
                                {currentUser?.username}
                            </p>
                            <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.outline, margin: 0, letterSpacing: '0.08em' }}>
                                {isAdmin ? 'ROLE_ADMIN' : 'ROLE_USER'}
                            </p>
                        </div>
                    )}
                </div>

                {/* Logout */}
                <button
                    onClick={handleLogout}
                    style={{
                        display: 'flex', alignItems: 'center', gap: '12px',
                        padding: '10px 12px', width: '100%',
                        background: 'none', border: 'none', cursor: 'pointer',
                        color: C.outline, transition: 'all 0.2s',
                        overflow: 'hidden', whiteSpace: 'nowrap',
                        borderRadius: '4px',
                    }}
                    onMouseEnter={e => { e.currentTarget.style.color = C.error; e.currentTarget.style.backgroundColor = 'rgba(255,180,171,0.06)'; }}
                    onMouseLeave={e => { e.currentTarget.style.color = C.outline; e.currentTarget.style.backgroundColor = 'transparent'; }}
                >
                    <span className="material-symbols-outlined" style={{ fontSize: '20px', flexShrink: 0 }}>logout</span>
                    {open && (
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                            Logout
                        </span>
                    )}
                </button>
            </div>

            <style>{`
                .material-symbols-outlined { font-variation-settings: 'FILL' 0, 'wght' 300; }
            `}</style>
        </aside>
    );
};

// ── Single sidebar link ───────────────────────────────────────────────────────
const SidebarLink = ({ to, icon, label, active, open, external }) => {
    const style = {
        display: 'flex', alignItems: 'center', gap: '12px',
        padding: '10px 12px',
        textDecoration: 'none',
        color: active ? '#f1bc8b' : '#d4c4b7',
        borderLeft: active ? '2px solid #f1bc8b' : '2px solid transparent',
        backgroundColor: active ? 'rgba(53,53,52,0.7)' : 'transparent',
        transition: 'all 0.2s',
        overflow: 'hidden', whiteSpace: 'nowrap',
        opacity: active ? 1 : 0.65,
        borderRadius: '0 4px 4px 0',
    };

    const content = (
        <>
            <span
                className="material-symbols-outlined"
                style={{ fontSize: '20px', flexShrink: 0, fontVariationSettings: active ? "'FILL' 1" : "'FILL' 0" }}
            >
                {icon}
            </span>
            {open && (
                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '11px', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                    {label}
                </span>
            )}
        </>
    );

    if (external) {
        return (
            <a
                href={to}
                target="_blank"
                rel="noopener noreferrer"
                style={style}
                onMouseEnter={e => { e.currentTarget.style.backgroundColor = 'rgba(42,42,42,0.6)'; e.currentTarget.style.opacity = '1'; }}
                onMouseLeave={e => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.opacity = '0.65'; }}
            >
                {content}
            </a>
        );
    }

    return (
        <Link
            to={to}
            style={style}
            onMouseEnter={e => { if (!active) { e.currentTarget.style.backgroundColor = 'rgba(42,42,42,0.6)'; e.currentTarget.style.opacity = '1'; } }}
            onMouseLeave={e => { if (!active) { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.opacity = '0.65'; } }}
        >
            {content}
        </Link>
    );
};

export default AppSidebar;
