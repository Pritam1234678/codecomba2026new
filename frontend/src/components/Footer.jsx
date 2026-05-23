import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import AuthService from '../services/auth.service';

const C = {
    bg:      '#131313',
    border:  '#50453b',
    primary: '#f1bc8b',
    muted:   '#d4c4b7',
    outline: '#9d8e83',
};

const Footer = () => {
    const [currentUser, setCurrentUser] = useState(undefined);

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) setCurrentUser(user);
    }, []);

    const isAdmin = currentUser?.roles?.includes('ROLE_ADMIN');

    return (
        <footer style={{
            backgroundColor: C.bg,
            borderTop: `1px solid ${C.border}`,
            width: '100%',
            fontFamily: "'Geist', sans-serif",
            position: 'relative',
            zIndex: 10,
        }}>
            {/* ── 3-column body ── */}
            <div style={{
                maxWidth: '1400px',
                margin: '0 auto',
                padding: '48px 64px 32px',
                display: 'grid',
                gridTemplateColumns: '2fr 1fr 1fr',
                gap: '48px',
            }}>
                {/* Brand */}
                <div>
                    <Link to="/" style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '22px',
                        fontWeight: 600,
                        color: C.primary,
                        fontStyle: 'italic',
                        textDecoration: 'none',
                        display: 'block',
                        marginBottom: '12px',
                    }}>
                        Code Coder
                    </Link>
                    <p style={{ fontSize: '15px', color: C.muted, lineHeight: 1.6, maxWidth: '320px' }}>
                        A competitive programming platform for students to practice and compete.
                    </p>
                </div>

                {/* Quick Links */}
                <div>
                    <h4 style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '12px',
                        letterSpacing: '0.1em',
                        color: C.muted,
                        textTransform: 'uppercase',
                        marginBottom: '16px',
                        marginTop: 0,
                    }}>
                        Quick Links
                    </h4>
                    <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        <li>
                            <FooterLink to={isAdmin ? '/admin/contests' : '/contests'}>Contests</FooterLink>
                        </li>
                        <li>
                            <FooterLink to={isAdmin ? '/admin/dashboard' : '/dashboard'}>Dashboard</FooterLink>
                        </li>
                        <li>
                            <FooterLink to="/support">Support</FooterLink>
                        </li>
                    </ul>
                </div>

                {/* Platform Info */}
                <div>
                    <h4 style={{
                        fontFamily: "'JetBrains Mono', monospace",
                        fontSize: '12px',
                        letterSpacing: '0.1em',
                        color: C.muted,
                        textTransform: 'uppercase',
                        marginBottom: '16px',
                        marginTop: 0,
                    }}>
                        Platform Info
                    </h4>
                    <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        {[
                            'Built with React & Spring Boot',
                            'Powered by Docker Judge System',
                            'Made by Pritam',
                        ].map(item => (
                            <li key={item} style={{
                                fontFamily: "'JetBrains Mono', monospace",
                                fontSize: '13px',
                                color: C.outline,
                                lineHeight: 1.5,
                            }}>
                                {item}
                            </li>
                        ))}
                    </ul>
                </div>
            </div>

            {/* ── Bottom bar ── */}
            <div style={{
                borderTop: `1px solid ${C.border}`,
                padding: '18px 64px',
                maxWidth: '1400px',
                margin: '0 auto',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '12px',
            }}>
                <p style={{
                    fontFamily: "'Geist', sans-serif",
                    fontSize: '13px',
                    color: C.muted,
                    margin: 0,
                }}>
                    © {new Date().getFullYear()} Code Coder. All rights reserved.
                </p>
                <p style={{
                    fontFamily: "'Geist', sans-serif",
                    fontSize: '13px',
                    color: C.muted,
                    margin: 0,
                }}>
                    Made with <span style={{ color: C.primary }}>❤</span> for competitive programmers
                </p>
            </div>
        </footer>
    );
};

const FooterLink = ({ to, children }) => (
    <Link
        to={to}
        style={{
            fontFamily: "'JetBrains Mono', monospace",
            fontSize: '13px',
            color: '#9d8e83',
            textDecoration: 'none',
            transition: 'color 0.2s',
        }}
        onMouseEnter={e => e.target.style.color = '#f1bc8b'}
        onMouseLeave={e => e.target.style.color = '#9d8e83'}
    >
        {children}
    </Link>
);

export default Footer;
