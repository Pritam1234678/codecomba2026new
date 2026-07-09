/**
 * Join Private Contest Page (Public)
 * 
 * Allows users to preview and join a private contest via invite link.
 * Accessible without authentication (preview), requires login to join.
 */

import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import PrivateContestService from '../services/privateContest.service';
import AuthService from '../services/auth.service';

const JoinPrivateContest = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');

    const [loading, setLoading] = useState(true);
    const [joining, setJoining] = useState(false);
    const [contest, setContest] = useState(null);
    const [error, setError] = useState('');
    const [joined, setJoined] = useState(false);

    const currentUser = AuthService.getCurrentUser();
    const isLoggedIn = !!currentUser;

    useEffect(() => {
        if (!token) {
            setError('Invalid invitation link');
            setLoading(false);
            return;
        }

        previewContest();
    }, [token]);

    const previewContest = async () => {
        try {
            setLoading(true);
            const response = await PrivateContestService.previewInvite(token);
            setContest(response.data);
        } catch (err) {
            setError(err.response?.data?.message || 'Invalid or expired invitation link');
        } finally {
            setLoading(false);
        }
    };

    const handleJoin = async () => {
        if (!isLoggedIn) {
            // Redirect to login with return URL
            localStorage.setItem('returnUrl', window.location.href);
            navigate('/login');
            return;
        }

        try {
            setJoining(true);
            setError('');
            const response = await PrivateContestService.acceptInvitation(token);
            setJoined(true);

            // Redirect to contest after 2 seconds
            setTimeout(() => {
                navigate(response.data.redirectUrl || `/contests/private/${response.data.contestId}/arena`);
            }, 2000);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to join contest');
            setJoining(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-[#131313] flex items-center justify-center">
                <div className="text-gray-400">Loading contest details...</div>
            </div>
        );
    }

    if (error && !contest) {
        return (
            <div className="min-h-screen bg-[#131313] text-gray-100 flex items-center justify-center p-8">
                <div className="max-w-lg w-full bg-[#1c1b1b] border border-red-500/30 rounded-xl p-8">
                    <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
                        <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </div>
                    <h2 className="text-2xl font-bold text-center mb-2 text-red-400">Invalid Invitation</h2>
                    <p className="text-gray-400 text-center mb-6">{error}</p>
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="w-full py-3 bg-gray-700 hover:bg-gray-600 rounded-lg font-semibold transition-colors"
                    >
                        Go to Dashboard
                    </button>
                </div>
            </div>
        );
    }

    // Success screen after joining
    if (joined) {
        return (
            <div className="min-h-screen bg-[#131313] text-gray-100 flex items-center justify-center p-8">
                <div className="max-w-lg w-full bg-[#1c1b1b] border border-green-500/30 rounded-xl p-8">
                    <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
                        <svg className="w-8 h-8 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                    </div>
                    <h2 className="text-2xl font-bold text-center mb-2 text-green-400">Successfully Joined!</h2>
                    <p className="text-gray-400 text-center mb-6">
                        You've been added to <strong className="text-gray-200">{contest.contestName}</strong>
                    </p>
                    <div className="text-center text-sm text-gray-500">
                        Redirecting to contest...
                    </div>
                </div>
            </div>
        );
    }

    // Contest preview and join interface
    const getStatusBadge = () => {
        const styles = {
            UPCOMING: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
            LIVE: 'bg-green-500/20 text-green-400 border-green-500/30 animate-pulse',
            ENDED: 'bg-gray-500/20 text-gray-400 border-gray-500/30'
        };

        return (
            <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${styles[contest.contestStatus] || styles.UPCOMING}`}>
                {contest.contestStatus}
            </span>
        );
    };

    const isContestFull = contest.participantCount >= contest.maxParticipants;
    const hasEnded = contest.contestStatus === 'ENDED';
    const canJoin = !isContestFull && !hasEnded;

    return (
        <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
            <div className="max-w-3xl mx-auto">
                <div className="bg-[#1c1b1b] rounded-xl border border-gray-800 overflow-hidden">
                    {/* Header */}
                    <div className="bg-gradient-to-r from-blue-600/20 to-purple-600/20 p-8 border-b border-gray-800">
                        <div className="flex items-center justify-between mb-4">
                            <h1 className="text-3xl font-bold">{contest.contestName}</h1>
                            {getStatusBadge()}
                        </div>
                        {contest.contestDescription && (
                            <p className="text-gray-300">{contest.contestDescription}</p>
                        )}
                    </div>

                    {/* Contest Details */}
                    <div className="p-8 space-y-6">
                        <div className="grid grid-cols-2 gap-6">
                            <div className="bg-[#0f0f0f] rounded-lg p-4">
                                <div className="text-gray-500 text-sm mb-1">Start Time</div>
                                <div className="text-gray-200 font-medium">
                                    {new Date(contest.startTime).toLocaleString()}
                                </div>
                            </div>
                            <div className="bg-[#0f0f0f] rounded-lg p-4">
                                <div className="text-gray-500 text-sm mb-1">End Time</div>
                                <div className="text-gray-200 font-medium">
                                    {new Date(contest.endTime).toLocaleString()}
                                </div>
                            </div>
                        </div>

                        <div className="bg-[#0f0f0f] rounded-lg p-4">
                            <div className="text-gray-500 text-sm mb-1">Hosted by</div>
                            <div className="text-gray-200 font-medium">{contest.hostUsername}</div>
                        </div>

                        <div className="flex items-center justify-between bg-[#0f0f0f] rounded-lg p-4">
                            <div>
                                <div className="text-gray-500 text-sm mb-1">Participants</div>
                                <div className="text-gray-200 font-medium">
                                    {contest.participantCount} / {contest.maxParticipants}
                                </div>
                            </div>
                            {isContestFull && (
                                <span className="px-3 py-1 bg-red-500/20 text-red-400 border border-red-500/30 rounded-full text-xs font-semibold">
                                    Contest Full
                                </span>
                            )}
                        </div>

                        {contest.enableProctoring && (
                            <div className="bg-purple-500/10 border border-purple-500/30 rounded-lg p-4">
                                <div className="flex items-start gap-3">
                                    <svg className="w-5 h-5 text-purple-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                    </svg>
                                    <div>
                                        <div className="font-semibold text-purple-400 mb-1">Proctored Contest</div>
                                        <p className="text-sm text-gray-300">
                                            This contest uses proctoring. You'll need to grant camera permission and
                                            agree to monitoring terms before participating.
                                        </p>
                                    </div>
                                </div>
                            </div>
                        )}

                        {error && (
                            <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 text-red-400">
                                {error}
                            </div>
                        )}

                        {/* Invitation Expiry */}
                        {contest.tokenExpiresAt && (
                            <div className="text-sm text-gray-500">
                                Invitation expires: {new Date(contest.tokenExpiresAt).toLocaleString()}
                            </div>
                        )}

                        {/* Join Button */}
                        <div className="pt-4">
                            {!isLoggedIn ? (
                                <div className="space-y-4">
                                    <button
                                        onClick={handleJoin}
                                        className="w-full py-3 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition-colors"
                                    >
                                        Login to Join Contest
                                    </button>
                                    <p className="text-center text-sm text-gray-500">
                                        Don't have an account?{' '}
                                        <button
                                            onClick={() => {
                                                localStorage.setItem('returnUrl', window.location.href);
                                                navigate('/register');
                                            }}
                                            className="text-blue-400 hover:text-blue-300"
                                        >
                                            Register here
                                        </button>
                                    </p>
                                </div>
                            ) : hasEnded ? (
                                <div className="bg-gray-700/30 border border-gray-700 rounded-lg p-4 text-center">
                                    <p className="text-gray-400">This contest has ended</p>
                                </div>
                            ) : isContestFull ? (
                                <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 text-center">
                                    <p className="text-red-400">Contest has reached maximum capacity</p>
                                </div>
                            ) : (
                                <button
                                    onClick={handleJoin}
                                    disabled={joining || !canJoin}
                                    className="w-full py-3 bg-green-600 hover:bg-green-700 disabled:bg-gray-700 disabled:cursor-not-allowed rounded-lg font-semibold transition-colors"
                                >
                                    {joining ? 'Joining Contest...' : 'Join Contest'}
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default JoinPrivateContest;
