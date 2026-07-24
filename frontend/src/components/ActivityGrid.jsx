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

export default function ActivityGrid({ userId }) {
    const { isMobile } = useResponsive();
    const [activity, setActivity] = useState({});
    const [hovered, setHovered] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchActivity = async () => {
            try {
                const [subs, practice, duels] = await Promise.all([
                    api.get('/submissions/user').catch(() => ({ data: [] })),
                    api.get('/practice/submissions/user').catch(() => ({ data: [] })),
                    api.get('/duel/history').catch(() => ({ data: [] })),
                ]);

                const days = {};
                const addDay = (dateStr) => {
                    if (!dateStr) return;
                    const d = dateStr.substring(0, 10);
                    days[d] = (days[d] || 0) + 1;
                };

                (subs.data || []).forEach(s => addDay(s.submittedAt));
                (practice.data || []).forEach(s => addDay(s.submittedAt));
                (duels.data || []).forEach(d => addDay(d.startedAt || d.createdAt));

                setActivity(days);
            } catch (e) { }
            setLoading(false);
        };
        fetchActivity();
    }, [userId]);

    const { weeks, months, totalDays, activeDays, currentStreak, maxCount } = useMemo(() => {
        const start = new Date('2026-05-01');
        const today = new Date();
        today.setHours(23, 59, 59, 999);

        const allDays = [];
        const monthsList = [];
        let current = new Date(start);
        let weekIndex = 0;
        let monthIndex = -1;
        let lastMonth = -1;

        while (current <= today) {
            const dateStr = current.toISOString().substring(0, 10);
            const month = current.getMonth();

            if (month !== lastMonth) {
                lastMonth = month;
                monthIndex++;
                const names = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
                monthsList.push({ index: weekIndex, label: names[month] });
            }

            const count = activity[dateStr] || 0;
            allDays.push({
                date: dateStr,
                count,
                level: count === 0 ? 0 : Math.min(5, Math.ceil(count / 2)),
                dayOfWeek: current.getDay(),
            });
            current.setDate(current.getDate() + 1);
        }

        // Pad start to align with Monday
        const startDayOfWeek = start.getDay();
        const padStart = startDayOfWeek === 0 ? 6 : startDayOfWeek - 1;
        for (let i = 0; i < padStart; i++) allDays.unshift(null);

        // Group into weeks
        const weeksArr = [];
        for (let i = 0; i < allDays.length; i += 7) {
            weeksArr.push(allDays.slice(i, i + 7));
        }

        const active = allDays.filter(d => d && d.count > 0).length;
        let streak = 0;
        const now = today.toISOString().substring(0, 10);
        for (let i = allDays.length - 1; i >= 0; i--) {
            const d = allDays[i];
            if (!d) continue;
            if (activity[d.date] && activity[d.date] > 0) streak++;
            else break;
        }

        const max = Math.max(1, ...Object.values(activity));

        return {
            weeks: weeksArr,
            months: monthsList,
            totalDays: allDays.filter(Boolean).length,
            activeDays: active,
            currentStreak: streak,
            maxCount: max,
        };
    }, [activity]);

    if (loading) return null;

    const dayLabels = ['', 'Mon', '', 'Wed', '', 'Fri', ''];

    return (
        <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            style={{ border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow, padding: isMobile ? '20px' : '28px 32px', marginBottom: '24px', position: 'relative', overflow: 'hidden' }}
        >
            {/* Decorative corner accent */}
            <div style={{ position: 'absolute', top: 0, right: 0, width: '120px', height: '120px', background: `radial-gradient(circle at 100% 0%, ${C.secondary}10, transparent 70%)`, pointerEvents: 'none' }} />

            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
                <div>
                    <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', letterSpacing: '0.15em', color: C.outline, textTransform: 'uppercase', display: 'block', marginBottom: '6px' }}>
                        Activity
                    </span>
                    <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '24px', fontWeight: 600, color: C.primary }}>
                        {activeDays} day{activeDays !== 1 ? 's' : ''} active
                    </span>
                </div>
                <div style={{ display: 'flex', gap: '24px' }}>
                    <div style={{ textAlign: 'right' }}>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', display: 'block' }}>Current Streak</span>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 300, color: C.secondary }}>
                            {currentStreak} day{currentStreak !== 1 ? 's' : ''}
                        </span>
                    </div>
                    <div style={{ textAlign: 'right' }}>
                        <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', letterSpacing: '0.1em', color: C.outline, textTransform: 'uppercase', display: 'block' }}>Total Submissions</span>
                        <span style={{ fontFamily: "'Playfair Display', serif", fontSize: '20px', fontWeight: 300, color: C.secondary }}>
                            {Object.values(activity).reduce((a, b) => a + b, 0)}
                        </span>
                    </div>
                </div>
            </div>

            {/* Grid */}
            <div style={{ overflowX: 'auto', paddingBottom: '8px' }}>
                {/* Month labels */}
                <div style={{ display: 'flex', marginLeft: '28px', marginBottom: '4px' }}>
                    {months.filter((_, i) => i === 0 || months[i].index - months[i-1].index >= 4).map((m, i) => (
                        <span key={i} style={{
                            fontFamily: "'JetBrains Mono', monospace", fontSize: '9px', color: C.outline,
                            letterSpacing: '0.06em', marginLeft: i === 0 ? `${m.index * 14 + 2}px` : `${(m.index - months[i-1].index) * 14 - 6}px`,
                        }}>
                            {m.label}
                        </span>
                    ))}
                </div>

                <div style={{ display: 'flex', gap: '2px' }}>
                    {/* Day labels */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', marginRight: '6px', paddingTop: '2px' }}>
                        {dayLabels.map((l, i) => (
                            <div key={i} style={{ width: '22px', height: '12px', display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}>
                                <span style={{ fontFamily: "'JetBrains Mono', monospace", fontSize: '7px', color: C.outline, letterSpacing: '0.04em' }}>{l}</span>
                            </div>
                        ))}
                    </div>

                    {/* Activity dots */}
                    <div style={{ display: 'flex', gap: '2px' }}>
                        {weeks.map((week, wi) => (
                            <div key={wi} style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                                {week.map((day, di) => {
                                    if (!day) return <div key={di} style={{ width: '12px', height: '12px' }} />;
                                    return (
                                        <motion.div
                                            key={day.date}
                                            initial={{ scale: 0, opacity: 0 }}
                                            animate={{ scale: 1, opacity: 1 }}
                                            transition={{ delay: wi * 0.003 + di * 0.001, duration: 0.3 }}
                                            whileHover={{ scale: 2.5, zIndex: 10, transition: { duration: 0.1 } }}
                                            onMouseEnter={() => setHovered(day)}
                                            onMouseLeave={() => setHovered(null)}
                                            style={{
                                                width: '12px', height: '12px',
                                                borderRadius: '50%',
                                                backgroundColor: INTENSITY[day.level],
                                                cursor: 'pointer',
                                                position: 'relative',
                                                zIndex: hovered?.date === day.date ? 10 : 0,
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

            {/* Tooltip */}
            {hovered && hovered.count > 0 && (
                <div style={{
                    fontFamily: "'JetBrains Mono', monospace", fontSize: '10px', color: C.onBg,
                    marginTop: '8px', padding: '4px 10px', display: 'inline-block',
                    border: `1px solid ${C.border}`, backgroundColor: C.surfaceLow,
                }}>
                    {new Date(hovered.date).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })}
                    {' — '}{hovered.count} submission{hovered.count > 1 ? 's' : ''}
                </div>
            )}
        </motion.div>
    );
}
