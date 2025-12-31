import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';
import api from '../services/api';

const ContestLeaderboard = () => {
    const { contestId } = useParams();
    const navigate = useNavigate();
    const [contest, setContest] = useState(null);
    const [leaderboard, setLeaderboard] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const headerRef = useRef(null);
    const tableRef = useRef(null);

    useEffect(() => {
        fetchLeaderboard();
    }, [contestId]);

    useEffect(() => {
        if (!loading && headerRef.current && tableRef.current) {
            const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });

            tl.from(headerRef.current, {
                opacity: 0,
                y: 30,
                duration: 0.8
            })
                .from(tableRef.current, {
                    opacity: 0,
                    y: 20,
                    duration: 0.6
                }, '-=0.3');
        }
    }, [loading]);

    const fetchLeaderboard = async () => {
        setLoading(true);
        setError(null);

        try {
            // Fetch contest details
            const contestRes = await api.get(`/contests/${contestId}`);
            setContest(contestRes.data);

            // Fetch leaderboard data
            const leaderboardRes = await api.get(`/admin/leaderboard/contest/${contestId}`);
            setLeaderboard(leaderboardRes.data);
            setLoading(false);
        } catch (err) {
            console.error('Error fetching leaderboard:', err);
            setError(err.response?.status === 403 ? 'Access denied - Admin only' : 'Failed to load leaderboard');
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-gray-400 text-lg">Loading...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 text-red-400">
                    {error}
                </div>
                <button
                    onClick={() => navigate('/admin/leaderboard')}
                    className="mt-4 text-green-400 hover:text-green-300 transition-colors"
                >
                    ← Back to Leaderboard
                </button>
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
            {/* Header */}
            <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-8 shadow-2xl">
                <button
                    onClick={() => navigate('/admin/leaderboard')}
                    className="text-green-400 hover:text-green-300 mb-4 flex items-center gap-2 transition-colors"
                >
                    ← Back to Contests
                </button>
                <h1 className="text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-1">
                    {contest?.name} - Leaderboard
                </h1>
                <p className="text-sm text-gray-500">Contest Rankings</p>
            </div>

            {/* Leaderboard Table */}
            <div ref={tableRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl overflow-hidden shadow-xl">
                <table className="w-full">
                    <thead className="bg-white/10 border-b border-white/20">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Rank</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Roll</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Problems Solved</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Total Score</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-white/10">
                        {leaderboard.length === 0 ? (
                            <tr>
                                <td colSpan="5" className="px-6 py-8 text-center text-gray-500">
                                    No submissions yet for this contest
                                </td>
                            </tr>
                        ) : (
                            leaderboard.map((entry, index) => (
                                <motion.tr
                                    key={entry.userId}
                                    initial={{ opacity: 0, x: -10 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    transition={{ delay: index * 0.05, duration: 0.3 }}
                                    whileHover={{ x: 3, backgroundColor: 'rgba(255, 255, 255, 0.05)' }}
                                    className={`transition-all ${index < 3 ? 'bg-white/5' : ''} hover:bg-white/10`}
                                >
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className={`text-2xl font-bold ${entry.rank === 1 ? 'text-yellow-400' :
                                            entry.rank === 2 ? 'text-gray-300' :
                                                entry.rank === 3 ? 'text-orange-400' :
                                                    'text-gray-500'
                                            }`}>
                                            {entry.rank === 1 ? '🥇' : entry.rank === 2 ? '🥈' : entry.rank === 3 ? '🥉' : entry.rank}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-gray-200 font-medium">{entry.userName || 'Unknown'}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-gray-400 font-mono text-sm">{entry.userRoll || 'N/A'}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-green-400 font-semibold">{entry.problemsSolved}</td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <span className="text-2xl font-bold text-green-400">{entry.totalScore.toFixed(1)}</span>
                                        <span className="text-gray-600 text-sm ml-1">/100</span>
                                    </td>
                                </motion.tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default ContestLeaderboard;
