/**
 * Manage Private Contest Page (Host Only)
 * 
 * Comprehensive contest management interface with tabs for:
 * - Overview (details, invite link, status)
 * - Problems (browse, attach, remove, AI generate)
 * - Participants (list, remove)
 * - Analytics (statistics, export)
 * - Proctoring (if enabled)
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import PrivateContestService from '../services/privateContest.service';

const ManagePrivateContest = () => {
    const { contestId } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [contest, setContest] = useState(null);
    const [activeTab, setActiveTab] = useState('overview');
    const [error, setError] = useState('');

    useEffect(() => {
        loadContest();
    }, [contestId]);

    const loadContest = async () => {
        try {
            setLoading(true);
            const response = await PrivateContestService.getContestDetails(contestId);
            setContest(response.data);
        } catch (err) {
            setError('Failed to load contest details');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-[#131313] flex items-center justify-center">
                <div className="text-gray-400">Loading contest...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-[#131313] p-8">
                <div className="max-w-4xl mx-auto">
                    <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-6 text-red-400">
                        {error}
                    </div>
                </div>
            </div>
        );
    }

    const tabs = [
        { id: 'overview', label: 'Overview', icon: '📊' },
        { id: 'problems', label: 'Problems', icon: '📝' },
        { id: 'participants', label: 'Participants', icon: '👥' },
        { id: 'analytics', label: 'Analytics', icon: '📈' },
    ];

    if (contest.enableProctoring) {
        tabs.push({ id: 'proctoring', label: 'Proctoring', icon: '👁️' });
    }

    return (
        <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="mb-8">
                    <button
                        onClick={() => navigate('/contests/private/my-contests')}
                        className="text-gray-400 hover:text-gray-300 mb-4 flex items-center gap-2"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                        </svg>
                        Back to My Contests
                    </button>

                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-3xl font-bold mb-2">{contest.name}</h1>
                            <p className="text-gray-400">Contest ID: {contest.id}</p>
                        </div>
                        <div className="flex items-center gap-3">
                            <span className={`px-4 py-2 rounded-lg font-semibold ${contest.status === 'UPCOMING' ? 'bg-blue-500/20 text-blue-400' :
                                    contest.status === 'LIVE' ? 'bg-green-500/20 text-green-400' :
                                        'bg-gray-500/20 text-gray-400'
                                }`}>
                                {contest.status}
                            </span>
                        </div>
                    </div>
                </div>

                {/* Tabs */}
                <div className="flex gap-2 mb-6 border-b border-gray-800">
                    {tabs.map(tab => (
                        <button
                            key={tab.id}
                            onClick={() => setActiveTab(tab.id)}
                            className={`px-6 py-3 font-semibold transition-all ${activeTab === tab.id
                                    ? 'text-blue-400 border-b-2 border-blue-400'
                                    : 'text-gray-400 hover:text-gray-300'
                                }`}
                        >
                            <span className="mr-2">{tab.icon}</span>
                            {tab.label}
                        </button>
                    ))}
                </div>

                {/* Tab Content */}
                <div className="bg-[#1c1b1b] rounded-xl border border-gray-800 p-8">
                    {activeTab === 'overview' && <OverviewTab contest={contest} onUpdate={loadContest} />}
                    {activeTab === 'problems' && <ProblemsTab contestId={contestId} contest={contest} />}
                    {activeTab === 'participants' && <ParticipantsTab contestId={contestId} contest={contest} />}
                    {activeTab === 'analytics' && <AnalyticsTab contestId={contestId} contest={contest} />}
                    {activeTab === 'proctoring' && <ProctoringTab contestId={contestId} contest={contest} />}
                </div>
            </div>
        </div>
    );
};

// Overview Tab Component
const OverviewTab = ({ contest, onUpdate }) => {
    const [copySuccess, setCopySuccess] = useState(false);

    const copyInviteLink = () => {
        navigator.clipboard.writeText(contest.inviteLink);
        setCopySuccess(true);
        setTimeout(() => setCopySuccess(false), 2000);
    };

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-2 gap-6">
                <div className="bg-[#0f0f0f] rounded-lg p-6">
                    <h3 className="font-semibold text-gray-300 mb-4">Contest Details</h3>
                    <div className="space-y-3 text-sm">
                        <div>
                            <span className="text-gray-500">Name:</span>
                            <p className="text-gray-200">{contest.name}</p>
                        </div>
                        <div>
                            <span className="text-gray-500">Description:</span>
                            <p className="text-gray-200">{contest.description || 'No description'}</p>
                        </div>
                        <div>
                            <span className="text-gray-500">Start Time:</span>
                            <p className="text-gray-200">{new Date(contest.startTime).toLocaleString()}</p>
                        </div>
                        <div>
                            <span className="text-gray-500">End Time:</span>
                            <p className="text-gray-200">{new Date(contest.endTime).toLocaleString()}</p>
                        </div>
                    </div>
                </div>

                <div className="bg-[#0f0f0f] rounded-lg p-6">
                    <h3 className="font-semibold text-gray-300 mb-4">Statistics</h3>
                    <div className="space-y-3 text-sm">
                        <div>
                            <span className="text-gray-500">Participants:</span>
                            <p className="text-gray-200 font-semibold">{contest.participantCount || 0}</p>
                        </div>
                        <div>
                            <span className="text-gray-500">Problems:</span>
                            <p className="text-gray-200 font-semibold">{contest.problemCount || 0}</p>
                        </div>
                        <div>
                            <span className="text-gray-500">Proctoring:</span>
                            <p className={`font-semibold ${contest.enableProctoring ? 'text-purple-400' : 'text-gray-400'}`}>
                                {contest.enableProctoring ? 'Enabled' : 'Disabled'}
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Invite Link */}
            <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-6">
                <h3 className="font-semibold text-blue-400 mb-3">Share Invite Link</h3>
                <div className="flex gap-2">
                    <input
                        type="text"
                        value={contest.inviteLink || 'Loading...'}
                        readOnly
                        className="flex-1 bg-[#0f0f0f] border border-gray-700 rounded-lg px-4 py-2 text-gray-300 font-mono text-sm"
                    />
                    <button
                        onClick={copyInviteLink}
                        className="px-6 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition-colors flex items-center gap-2"
                    >
                        {copySuccess ? (
                            <>
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                                Copied!
                            </>
                        ) : (
                            <>
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                </svg>
                                Copy Link
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};

// Problems Tab - placeholder (full implementation would be larger)
const ProblemsTab = ({ contestId, contest }) => {
    return (
        <div className="text-center py-12">
            <p className="text-gray-400">Problems management interface</p>
            <p className="text-sm text-gray-500 mt-2">Browse, attach, and manage contest problems</p>
        </div>
    );
};

// Participants Tab - placeholder
const ParticipantsTab = ({ contestId, contest }) => {
    return (
        <div className="text-center py-12">
            <p className="text-gray-400">Participants management interface</p>
            <p className="text-sm text-gray-500 mt-2">View and manage contest participants</p>
        </div>
    );
};

// Analytics Tab - placeholder
const AnalyticsTab = ({ contestId, contest }) => {
    return (
        <div className="text-center py-12">
            <p className="text-gray-400">Analytics and reporting interface</p>
            <p className="text-sm text-gray-500 mt-2">View contest statistics and export data</p>
        </div>
    );
};

// Proctoring Tab - placeholder
const ProctoringTab = ({ contestId, contest }) => {
    return (
        <div className="text-center py-12">
            <p className="text-gray-400">Proctoring monitoring interface</p>
            <p className="text-sm text-gray-500 mt-2">View proctoring sessions and flagged activities</p>
        </div>
    );
};

export default ManagePrivateContest;
