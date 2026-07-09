/**
 * Private Contest List Page
 * 
 * Shows two tabs:
 * 1. My Contests - Contests where user is the host
 * 2. Joined Contests - Contests where user is a participant
 */

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import PrivateContestService from '../services/privateContest.service';

const PrivateContestList = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('hosted'); // 'hosted' or 'joined'
    const [loading, setLoading] = useState(true);
    const [hostedContests, setHostedContests] = useState([]);
    const [joinedContests, setJoinedContests] = useState([]);
    const [error, setError] = useState('');

    useEffect(() => {
        loadContests();
    }, []);

    const loadContests = async () => {
        try {
            setLoading(true);
            const [hostedRes, joinedRes] = await Promise.all([
                PrivateContestService.getMyContests(),
                PrivateContestService.getJoinedContests()
            ]);
            setHostedContests(hostedRes.data);
            setJoinedContests(joinedRes.data);
        } catch (err) {
            setError('Failed to load contests');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const getStatusBadge = (status) => {
        const styles = {
            UPCOMING: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
            LIVE: 'bg-green-500/20 text-green-400 border-green-500/30',
            ENDED: 'bg-gray-500/20 text-gray-400 border-gray-500/30'
        };

        return (
            <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${styles[status] || styles.UPCOMING}`}>
                {status}
            </span>
        );
    };

    const formatDate = (dateStr) => {
        return new Date(dateStr).toLocaleString(undefined, {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    const ContestCard = ({ contest, isHost }) => (
        <div
            onClick={() => navigate(isHost ? `/contests/private/${contest.id}/manage` : `/contests/private/${contest.id}/arena`)}
            className="bg-[#1c1b1b] border border-gray-800 rounded-xl p-6 hover:border-blue-500/50 transition-all cursor-pointer group"
        >
            <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-xl font-bold text-gray-100 group-hover:text-blue-400 transition-colors">
                            {contest.name}
                        </h3>
                        {getStatusBadge(contest.status)}
                    </div>
                    {contest.description && (
                        <p className="text-gray-400 text-sm line-clamp-2">{contest.description}</p>
                    )}
                </div>
                {isHost && (
                    <button
                        onClick={(e) => {
                            e.stopPropagation();
                            navigate(`/contests/private/${contest.id}/manage`);
                        }}
                        className="ml-4 p-2 text-gray-400 hover:text-blue-400 hover:bg-blue-500/10 rounded-lg transition-colors"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        </svg>
                    </button>
                )}
            </div>

            <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                    <span className="text-gray-500">Start:</span>
                    <p className="text-gray-300 font-medium">{formatDate(contest.startTime)}</p>
                </div>
                <div>
                    <span className="text-gray-500">End:</span>
                    <p className="text-gray-300 font-medium">{formatDate(contest.endTime)}</p>
                </div>
            </div>

            <div className="mt-4 pt-4 border-t border-gray-800 flex items-center justify-between text-sm">
                <div className="flex items-center gap-4">
                    <div className="flex items-center gap-2 text-gray-400">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                        </svg>
                        <span>{contest.participantCount || 0} participants</span>
                    </div>
                    {contest.enableProctoring && (
                        <div className="flex items-center gap-2 text-purple-400">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                            </svg>
                            <span>Proctored</span>
                        </div>
                    )}
                </div>
                {isHost ? (
                    <span className="text-blue-400 font-medium">Manage →</span>
                ) : (
                    <span className="text-green-400 font-medium">Enter Contest →</span>
                )}
            </div>
        </div>
    );

    if (loading) {
        return (
            <div className="min-h-screen bg-[#131313] text-gray-100 p-8 flex items-center justify-center">
                <div className="text-gray-400">Loading contests...</div>
            </div>
        );
    }

    const displayContests = activeTab === 'hosted' ? hostedContests : joinedContests;

    return (
        <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h1 className="text-3xl font-bold mb-2">Private Contests</h1>
                        <p className="text-gray-400">
                            Manage your hosted contests or participate in invited contests
                        </p>
                    </div>
                    {activeTab === 'hosted' && (
                        <button
                            onClick={() => navigate('/contests/private/create')}
                            className="px-6 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition-colors flex items-center gap-2"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                            </svg>
                            Create Contest
                        </button>
                    )}
                </div>

                {/* Tabs */}
                <div className="flex gap-4 mb-6 border-b border-gray-800">
                    <button
                        onClick={() => setActiveTab('hosted')}
                        className={`px-6 py-3 font-semibold transition-all ${activeTab === 'hosted'
                                ? 'text-blue-400 border-b-2 border-blue-400'
                                : 'text-gray-400 hover:text-gray-300'
                            }`}
                    >
                        My Contests ({hostedContests.length})
                    </button>
                    <button
                        onClick={() => setActiveTab('joined')}
                        className={`px-6 py-3 font-semibold transition-all ${activeTab === 'joined'
                                ? 'text-blue-400 border-b-2 border-blue-400'
                                : 'text-gray-400 hover:text-gray-300'
                            }`}
                    >
                        Joined Contests ({joinedContests.length})
                    </button>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 text-red-400 mb-6">
                        {error}
                    </div>
                )}

                {/* Contest List */}
                {displayContests.length === 0 ? (
                    <div className="bg-[#1c1b1b] border border-gray-800 rounded-xl p-12 text-center">
                        <div className="w-20 h-20 bg-gray-800 rounded-full flex items-center justify-center mx-auto mb-4">
                            <svg className="w-10 h-10 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                        </div>
                        <h3 className="text-xl font-bold text-gray-300 mb-2">
                            {activeTab === 'hosted' ? 'No Contests Created Yet' : 'No Contests Joined Yet'}
                        </h3>
                        <p className="text-gray-500 mb-6">
                            {activeTab === 'hosted'
                                ? 'Create your first private contest to get started'
                                : 'Join a contest using an invite link to participate'}
                        </p>
                        {activeTab === 'hosted' && (
                            <button
                                onClick={() => navigate('/contests/private/create')}
                                className="px-6 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition-colors inline-flex items-center gap-2"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                                </svg>
                                Create Contest
                            </button>
                        )}
                    </div>
                ) : (
                    <div className="grid gap-6">
                        {displayContests.map((contest) => (
                            <ContestCard
                                key={contest.id}
                                contest={contest}
                                isHost={activeTab === 'hosted'}
                            />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default PrivateContestList;
