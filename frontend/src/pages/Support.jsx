import React, { useState } from 'react';
import { motion } from 'framer-motion';

const C = {
    bg:        '#131313',
    surfaceLow:'#1c1b1b',
    surfaceCon:'#201f1f',
    border:    '#50453b',
    primary:   '#f1bc8b',
    secondary: '#e9c176',
    muted:     '#d4c4b7',
    outline:   '#9d8e83',
    error:     '#ffb4ab',
    onBg:      '#e5e2e1',
};

// ── Underline input ───────────────────────────────────────────────────────────
const UInput = ({ type = 'text', placeholder, name, value, onChange, required }) => (
    <input
        type={type} placeholder={placeholder} name={name}
        value={value} onChange={onChange} required={required}
        style={{
            width: '100%', backgroundColor: 'transparent',
            border: 'none', borderBottom: `1px solid ${C.border}`,
            color: C.onBg, fontFamily: "'Geist', sans-serif",
            fontSize: '16px', lineHeight: 1.5,
            padding: '16px 0', outline: 'none',
            transition: 'border-color 0.2s', boxSizing: 'border-box',
        }}
        onFocus={e => e.target.style.borderBottomColor = C.secondary}
        onBlur={e => e.target.style.borderBottomColor = C.border}
    />
);

// ── FAQ accordion item ────────────────────────────────────────────────────────
const FaqItem = ({ question, answer, isLast }) => {
    const [open, setOpen] = useState(false);
    return (
        <div style={{ borderTop: `1px solid ${C.border}`, ...(isLast ? { borderBottom: `1px solid ${C.border}` } : {}) }}>
            <button
                onClick={() => setOpen(!open)}
                style={{
                    width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                    padding: '24px 0', background: 'none', border: 'none', cursor: 'pointer',
                    fontFamily: "'Geist', sans-serif", fontSize: '18px', lineHeight: 1.6,
                    color: C.onBg, textAlign: 'left',
                }}
            >
                <span>{question}</span>
                <span style={{
                    color: C.secondary, fontSize: '20px', fontWeight: 300,
                    transform: open ? 'rotate(180deg)' : 'rotate(0deg)',
                    transition: 'transform 0.3s',
                    flexShrink: 0, marginLeft: '16px',
                }}>
                    ∨
                </span>
            </button>
            {open && (
                <p style={{
                    fontFamily: "'Geist', sans-serif", fontSize: '16px', lineHeight: 1.5,
                    color: C.muted, paddingBottom: '24px', paddingRight: '32px', margin: 0,
                }}>
                    {answer}
                </p>
            )}
        </div>
    );
};

// ── Category card ─────────────────────────────────────────────────────────────
const CategoryCard = ({ label }) => {
    const [hovered, setHovered] = useState(false);
    return (
        <div
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'flex', alignItems: 'center', gap: '16px',
                border: `1px solid ${hovered ? C.secondary : C.border}`,
                padding: '16px',
                backgroundColor: hovered ? C.surfaceLow : 'transparent',
                transition: 'all 0.2s', cursor: 'pointer',
            }}
        >
            <span style={{
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px', letterSpacing: '0.1em',
                color: C.onBg, textTransform: 'uppercase',
            }}>
                {label}
            </span>
        </div>
    );
};

const Support = () => {
    const [formData, setFormData] = useState({ fullName: '', email: '', phone: '', message: '' });
    const [submitted, setSubmitted] = useState(false);
    const [loading, setLoading]     = useState(false);
    const [error, setError]         = useState('');

    const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true); setError('');
        try {
            const res = await fetch('/api/support/send', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData),
            });
            if (res.ok) {
                setSubmitted(true);
                setTimeout(() => {
                    setSubmitted(false);
                    setFormData({ fullName: '', email: '', phone: '', message: '' });
                }, 4000);
            } else {
                setError('Failed to send message. Please try again.');
            }
        } catch {
            setError('Error sending message. Please try again later.');
        } finally {
            setLoading(false);
        }
    };

    const faqs = [
        {
            question: 'What is the standard response time?',
            answer: 'Standard inquiries are processed within 24-48 hours. Active contest disputes are routed to high-priority channels for immediate architectural review.',
        },
        {
            question: 'How do I report a platform bug?',
            answer: "Use the support form and describe the issue in detail. Include reproduction steps, environment details, and any relevant error messages.",
        },
        {
            question: 'Can I appeal a submission verdict?',
            answer: 'Verdict appeals are only considered if a proven issue exists with the test cases or execution environment. Logic errors are final.',
        },
        {
            question: 'What programming languages are supported?',
            answer: 'We support C, C++, Java, Python, and JavaScript. Choose the language you\'re most comfortable with.',
        },
        {
            question: 'Is there a registration fee?',
            answer: 'No, Code Combat is completely free to participate. This is our way of giving back to the programming community.',
        },
    ];

    return (
        <div style={{
            backgroundColor: C.bg, color: C.onBg,
            minHeight: '100vh', fontFamily: "'Geist', sans-serif",
        }}>
            <main style={{ padding: '2rem 64px 4rem' }}>

                {/* ── Header ── */}
                <motion.header
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6 }}
                    style={{ marginBottom: '2rem', maxWidth: '66%' }}
                >
                    <h1 style={{
                        fontFamily: "'Playfair Display', serif",
                        fontSize: '48px', fontWeight: 700,
                        lineHeight: 1.1, letterSpacing: '-0.02em',
                        color: C.onBg, marginBottom: '1rem',
                    }}>
                        Command<br />Assistance.
                    </h1>
                    <p style={{ fontSize: '18px', lineHeight: 1.6, color: C.muted, maxWidth: '640px', margin: 0 }}>
                        Submit an inquiry directly to the command center. Architectural issues, account anomalies, and contest disputes are prioritized.
                    </p>
                </motion.header>

                {/* ── 12-col grid ── */}
                <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(12, 1fr)',
                    gap: '32px',
                }}>
                    {/* ── Left: Form (8 cols) ── */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.1 }}
                        style={{
                            gridColumn: 'span 8',
                            borderTop: `1px solid ${C.border}`,
                            paddingTop: '3rem',
                            display: 'flex', flexDirection: 'column', gap: '3rem',
                        }}
                    >
                        {/* Success state */}
                        {submitted && (
                            <motion.div
                                initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}
                                style={{
                                    backgroundColor: C.surfaceLow,
                                    borderLeft: `2px solid ${C.primary}`,
                                    padding: '24px',
                                    display: 'flex', alignItems: 'flex-start', gap: '16px',
                                }}
                            >
                                <div>
                                    <h4 style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.primary, textTransform: 'uppercase', marginBottom: '8px' }}>
                                        Ticket Created
                                    </h4>
                                    <p style={{ fontSize: '16px', color: C.muted, margin: 0, lineHeight: 1.5 }}>
                                        Your transmission has been logged. An architect will respond within standard operational cycles.
                                    </p>
                                </div>
                            </motion.div>
                        )}

                        {/* Error */}
                        {error && (
                            <div style={{ padding: '12px 16px', border: `1px solid ${C.error}`, borderLeft: `3px solid ${C.error}`, backgroundColor: 'rgba(255,180,171,0.08)' }}>
                                <p style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', color: C.error, margin: 0 }}>{error}</p>
                            </div>
                        )}

                        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '2rem', maxWidth: '768px' }}>
                            {/* Name + Email row */}
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.muted, textTransform: 'uppercase' }}>
                                        Operative Name
                                    </label>
                                    <UInput name="fullName" placeholder="Enter designation" value={formData.fullName} onChange={handleChange} required />
                                </div>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                    <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.muted, textTransform: 'uppercase' }}>
                                        Comm Link (Email)
                                    </label>
                                    <UInput name="email" type="email" placeholder="user@domain.com" value={formData.email} onChange={handleChange} required />
                                </div>
                            </div>

                            {/* Message */}
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '1rem' }}>
                                <label style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '12px', letterSpacing: '0.1em', color: C.muted, textTransform: 'uppercase' }}>
                                    Message Payload
                                </label>
                                <textarea
                                    name="message"
                                    placeholder="Detail your inquiry..."
                                    rows={6}
                                    value={formData.message}
                                    onChange={handleChange}
                                    required
                                    style={{
                                        width: '100%', backgroundColor: 'transparent',
                                        border: `1px solid ${C.border}`,
                                        color: C.onBg, fontFamily: "'Geist', sans-serif",
                                        fontSize: '16px', lineHeight: 1.5,
                                        padding: '16px', outline: 'none', resize: 'none',
                                        transition: 'border-color 0.2s', boxSizing: 'border-box',
                                        marginTop: '8px',
                                    }}
                                    onFocus={e => e.target.style.borderColor = C.secondary}
                                    onBlur={e => e.target.style.borderColor = C.border}
                                />
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '13px', color: C.muted, opacity: 0.5, textAlign: 'right', marginTop: '8px' }}>
                                    Target: support@codecombat.live
                                </span>
                            </div>

                            {/* Submit */}
                            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1rem' }}>
                                <TransmitButton disabled={loading}>
                                    {loading ? 'Transmitting...' : 'Transmit Log'}
                                </TransmitButton>
                            </div>
                        </form>
                    </motion.div>

                    {/* ── Right: Info & FAQ (4 cols) ── */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.2 }}
                        style={{
                            gridColumn: 'span 4',
                            borderLeft: `1px solid ${C.border}`,
                            paddingLeft: '32px',
                            display: 'flex', flexDirection: 'column', gap: '4rem',
                        }}
                    >
                        {/* Categories */}
                        <section style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '32px', fontWeight: 600, color: C.onBg, margin: 0 }}>
                                Categories
                            </h3>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                                <CategoryCard label="Technical" />
                                <CategoryCard label="Account" />
                                <CategoryCard label="Contests" />
                            </div>
                        </section>

                        {/* FAQ */}
                        <section style={{ display: 'flex', flexDirection: 'column' }}>
                            <h3 style={{ fontFamily: "'Playfair Display', serif", fontSize: '32px', fontWeight: 600, color: C.onBg, marginBottom: '2rem', marginTop: 0 }}>
                                FAQ
                            </h3>
                            {faqs.map((faq, i) => (
                                <FaqItem key={i} question={faq.question} answer={faq.answer} isLast={i === faqs.length - 1} />
                            ))}
                        </section>
                    </motion.div>
                </div>
            </main>
        </div>
    );
};

// ── Transmit button ───────────────────────────────────────────────────────────
const TransmitButton = ({ children, disabled }) => {
    const [hovered, setHovered] = React.useState(false);
    return (
        <button
            type="submit"
            disabled={disabled}
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            style={{
                display: 'flex', alignItems: 'center', gap: '12px',
                border: `1px solid ${C.secondary}`,
                padding: '16px 32px',
                backgroundColor: hovered && !disabled ? C.secondary : 'transparent',
                color: hovered && !disabled ? '#131313' : C.secondary,
                fontFamily: "'JetBrains Mono', monospace",
                fontSize: '12px', letterSpacing: '0.1em',
                fontWeight: 500, textTransform: 'uppercase',
                cursor: disabled ? 'not-allowed' : 'pointer',
                opacity: disabled ? 0.5 : 1,
                transition: 'all 0.3s',
            }}
        >
            {children}
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
        </button>
    );
};

export default Support;
