import { useState, useEffect, useMemo } from 'react';
import { motion } from 'framer-motion';
import api from '../services/api';
import useResponsive from '../hooks/useResponsive';

const C = {
    bg: '#131313', surfaceLow: '#1c1b1b', border: 'rgba(241,188,139,0.2)',
    primary: '#f1bc8b', secondary: '#e9c176', muted: '#d4c4b7', outline: '#9d8e83', onBg: '#e5e2e1',
};

const INTENSITY = [
    'rgba(241,188,139,0.06)',
    'rgba(241,188,139,0.18)',
    'rgba(241,188,139,0.35)',
    'rgba(233,193,118,0.55)',
    'rgba(233,193,118,0.78)',
    'rgba(233,193,118,1)',
];

const DAY_LABELS = ['Mon', '', 'Wed', '', 'Fri', '', 'Sun'];
const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export default function ActivityGrid({ userId }) {
    const { isMobile } = useResponsive();
    const [activity, setActivity] = useState({});
    const [hovered, setHovered] = useState(null);
    const [hoverPos, setHoverPos] = useState({ x: 0, y: 0 });
    const [loading, setLoading] = useState(true);
    const [maxStreak, setMaxStreak] = useState(0);

    useEffect(() => {
        const fetchActivity = async () => {
            try {
                const [subs, practice, duels] = await Promise.all([
                    api.get('/submissions/user').catch(() => ({ data: [] })),
                    api.get('/practice/submissions/user').catch(() => ({ data: [] })),
                    api.get('/user/duel-history').catch(() => ({ data: [] })),
                ]);

                const days = {};
                const addEntry = (dateStr, type) => {
                    if (!dateStr) return;
                    const d = dateStr.substring(0, 10);
                    if (!days[d]) days[d] = { total: 0, practice: 0, contest: 0, duel: 0 };
                    days[d].total++;
                    days[d][type]++;
                };

                (subs.data || []).forEach(s => addEntry(s.submittedAt, 'contest'));
                (practice.data || []).forEach(s => addEntry(s.submittedAt, 'practice'));
                (duels.data || []).forEach(d => addEntry(d.endedAt, 'duel'));

                setActivity(days);
            } catch (e) { }
            // Fetch user profile for max streak
            try {
                const pr = await api.get('/user/profile');
                setMaxStreak(pr.data?.maxStreak || 0);
            } catch (e) { }
            setLoading(false);
        };
        fetchActivity();
    }, [userId]);

    const { weeks, months, monthGaps, totalDays, activeDays, currentStreak } = useMemo(() => {
        const start = new Date('2026-05-01');
        const today = new Date();
        today.setHours(23, 59, 59, 999);

        const allDays = [];
        let current = new Date(start);

        while (current <= today) {
            const dateStr = current.toISOString().substring(0, 10);
            const data = activity[dateStr];
            const count = data ? data.total : 0;
            const level = count === 0 ? 0 : Math.min(5, Math.ceil(count / 2));
            allDays.push({ date: dateStr, count, level, data: data || { total: 0, practice: 0, contest: 0, duel: 0 } });
            current.setDate(current.getDate() + 1);
        }

        // Pad to align Monday start
        const startDOW = start.getDay();
        const padStart = startDOW === 0 ? 6 : startDOW - 1;
        for (let i = 0; i < padStart; i++) allDays.unshift(null);

        // Group into weeks (Monday-Sunday)
        const weeksArr = [];
        for (let i = 0; i < allDays.length; i += 7) {
            const week = allDays.slice(i, i + 7);
            while (week.length < 7) week.push(null);
            weeksArr.push({ days: week, index: weeksArr.length });
        }

        // Month positions with week gaps
        const monthsArr = [];
        let lastMonth = -1;
        const monthGaps = new Set(); // week indices where a gap should appear
        weeksArr.forEach((week, wi) => {
            const firstDay = week.days.find(d => d !== null);
            if (firstDay) {
                const m = parseInt(firstDay.date.substring(5, 7)) - 1;
                if (m !== lastMonth) {
                    if (lastMonth !== -1) monthGaps.add(wi);
                    lastMonth = m;
                    monthsArr.push({ weekIndex: wi, label: MONTH_NAMES[m] });
                }
            }
        });

        const active = allDays.filter(d => d && d.count > 0).length;

        // Streak: count consecutive days from the most recent activity day backwards
        let streak = 0;
        const todayStr = today.toISOString().substring(0, 10);
        
        // Find the most recent day with activity (starting from today backwards)
        let lastActiveIndex = -1;
        for (let i = allDays.length - 1; i >= 0; i--) {
            const d = allDays[i];
            if (!d) continue;
            if (activity[d.date] && activity[d.date].total > 0) {
                lastActiveIndex = i;
                break;
            }
        }
        
        // Count consecutive days backwards from the last active day
        if (lastActiveIndex !== -1) {
            for (let i = lastActiveIndex; i >= 0; i--) {
                const d = allDays[i];
                if (!d) break;
                if (activity[d.date] && activity[d.date].total > 0) streak++;
                else break;
            }
        }

        return { weeks: weeksArr, months: monthsArr, monthGaps, totalDays: allDays.filter(Boolean).length, activeDays: active, currentStreak: streak };
    }, [activity]);

    if (loading) return null;

    const totalSubs = Object.values(activity).reduce((a, b) => a + b.total, 0);

    return (
        <motion.div
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6, delay: 0.1 }}
            style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: isMobile ? '20px' : '28px 32px', marginBottom: '24px', position: 'relative', overflow: 'hidden' }}
        >
            <div style={{ position: 'absolute', top: 0, right: 0, width: '140px', height: '140px', background: `radial-gradient(circle at 100% 0%, ${C.secondary}10, transparent 70%)`, pointerEvents: 'none' }} />

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
                <div>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '6px' }}>Activity</span>
                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.primary }}>{activeDays} day{activeDays !== 1 ? 's' : ''} active</span>
                </div>
                <div style={{ display: 'flex', gap: '28px' }}>
                    <div style={{ textAlign: 'right' }}>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', display: 'block' }}>Current</span>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 300, color: C.secondary }}>{currentStreak}d</span>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', display: 'block' }}>Best</span>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 300, color: C.secondary }}>{maxStreak}d</span>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', display: 'block' }}>Submissions</span>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 300, color: C.secondary }}>{totalSubs}</span>
                    </div>
                </div>
            </div>

            <div style={{ overflowX: 'auto', paddingBottom: '4px' }}>
                {/* Month labels */}
                <div style={{ display: 'flex', marginLeft: '30px', marginBottom: '6px', height: '16px', position: 'relative' }}>
                    {months.map((m, i) => {
                        const left = 30 + m.weekIndex * 14;
                        const prevLeft = i > 0 ? 30 + months[i - 1].weekIndex * 14 : -100;
                        const gap = left - prevLeft;
                        return (
                            <span key={i} style={{
                                position: 'absolute', left: `${left}px`,
                                fontFamily: "'JetBrains Mono', monospace", fontSize: '9px',
                                color: C.outline, letterSpacing: '0.06em',
                                minWidth: gap > 60 ? `${Math.min(gap - 4, 80)}px` : 'auto',
                            }}>
                                {gap > 30 ? m.label : ''}
                            </span>
                        );
                    })}
                </div>

                <div style={{ display: 'flex', gap: '3px' }}>
                    {/* Day labels */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '3px', marginRight: '4px', paddingTop: '2px' }}>
                        {DAY_LABELS.map((l, i) => (
                            <div key={i} style={{ width: '24px', height: '12px', display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}>
                                {l ? <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '7px', color: C.outline, letterSpacing: '0.04em' }}>{l}</span> : null}
                            </div>
                        ))}
                    </div>

                    {/* Dots */}
                    <div style={{ display: 'flex', gap: '3px' }}>
                        {weeks.map((week, wi) => (
                            <div key={wi} style={{ display: 'flex', flexDirection: 'column', gap: '3px', marginRight: monthGaps.has(wi) ? '6px' : '0px' }}>
                                {week.days.map((day, di) => {
                                    if (!day) return <div key={di} style={{ width: '12px', height: '12px' }} />;
                                    return (
                                        <motion.div
                                            key={day.date}
                                            initial={{ scale: 0, opacity: 0 }}
                                            animate={{ scale: 1, opacity: 1 }}
                                            transition={{ delay: wi * 0.004 + di * 0.002, duration: 0.25 }}
                                            whileHover={{ scale: 2.8, zIndex: 20, transition: { duration: 0.1 } }}
                                            onMouseEnter={e => {
                                                setHovered(day);
                                                setHoverPos({ x: e.clientX, y: e.clientY });
                                            }}
                                            onMouseLeave={() => setHovered(null)}
                                            style={{
                                                width: '12px', height: '12px', borderRadius: '50%',
                                                backgroundColor: INTENSITY[day.level],
                                                cursor: 'pointer', position: 'relative',
                                                zIndex: hovered?.date === day.date ? 20 : 0,
                                            }}
                                        />
                                    );
                                })}
                            </div>
                        ))}
                    </div>
                </div>

                {/* Legend */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '4px', marginTop: '10px' }}>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', color: C.outline, marginRight: '4px' }}>Less</span>
                    {INTENSITY.map((color, i) => (
                        <div key={i} style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: color }} />
                    ))}
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '8px', color: C.outline, marginLeft: '4px' }}>More</span>
                </div>
            </div>

            {/* Hover tooltip */}
            {hovered && (
                <div style={{
                    position: 'fixed', left: `${hoverPos.x + 14}px`, top: `${hoverPos.y - 10}px`,
                    zIndex: 100, pointerEvents: 'none',
                    border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow,
                    padding: '10px 14px', borderRadius: '4px', boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
                    fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.onBg,
                    display: 'flex', flexDirection: 'column', gap: '4px', minWidth: '180px',
                }}>
                    <span style={{ color: C.primary, fontWeight: 600, fontSize: '11px' }}>
                        {new Date(hovered.date + 'T00:00:00').toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric', year: 'numeric' })}
                    </span>
                    <span style={{ color: C.muted }}>{hovered.count} total submission{hovered.count !== 1 ? 's' : ''}</span>
                    {hovered.data.contest > 0 && (
                        <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.secondary }} />
                            {hovered.data.contest} contest
                        </span>
                    )}
                    {hovered.data.practice > 0 && (
                        <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: C.primary }} />
                            {hovered.data.practice} practice
                        </span>
                    )}
                    {hovered.data.duel > 0 && (
                        <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                            <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: '#f4bb92' }} />
                            {hovered.data.duel} duel
                        </span>
                    )}
                </div>
            )}
        </motion.div>
    );
}
