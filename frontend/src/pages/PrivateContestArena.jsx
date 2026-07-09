/**
 * Private Contest Arena (Participant View)
 * 
 * Main interface for participants to:
 * - View contest problems
 * - Write and submit code
 * - See real-time leaderboard
 * - Track their progress
 */

import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import PrivateContestService from '../services/privateContest.service';
import Editor from '@monaco-editor/react';

const PrivateContestArena = () => {
    const { contestId } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [contest, setContest] = useState(null);
    const [selectedProblem, setSelectedProblem] = useState(null);
    const [code, setCode] = useState('');
    const [language, setLanguage] = useState('java');
    const [submitting, setSubmitting] = useState(false);
    const [showLeaderboard, setShowLeaderboard] = useState(false);
    const [leaderboard, setLeaderboard] = useState([]);
    const [error, setError] = useState('');

    useEffect(() => {
        loadContest();
        // Auto-refresh leaderboard every 30 seconds
        const interval = setInterval(loadLeaderboard, 30000);
        return () => clearInterval(interval);
    }, [contestId]);

    useEffect(() => {
        if (selectedProblem) {
            loadLeaderboard();
        }
    }, [selectedProblem]);

    const loadContest = async () => {
        try {
            setLoading(true);
            const response = await PrivateContestService.getContestDetails(contestId);
            setContest(response.data);

            // Select first problem by default
            if (response.data.problems && response.data.problems.length > 0) {
                setSelectedProblem(response.data.problems[0]);
            }
        } catch (err) {
            setError('Failed to load contest');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const loadLeaderboard = async () => {
        try {
            const response = await PrivateContestService.getLeaderboard(contestId);
            setLeaderboard(response.data);
        } catch (err) {
            console.error('Failed to load leaderboard:', err);
        }
    };

    const handleSubmit = async () => {
        if (!code.trim()) {
            alert('Please write some code before submitting');
            return;
        }

        try {
            setSubmitting(true);
            setError('');

            await PrivateContestService.submitCode(contestId, {
                problemId: selectedProblem.id,
                code,
                language
            });

            alert('Code submitted successfully! Verdict will be delivered shortly.');

            // Reload leaderboard after submission
            setTimeout(loadLeaderboard, 2000);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to submit code');
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-[#131313] flex items-center justify-center">
                <div className="text-gray-400">Loading contest...</div>
            </div>
        );
    }

    if (error && !contest) {
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

    // Check if contest is live
    if (contest.status !== 'LIVE') {
        return (
            <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
                <div className="max-w-4xl mx-auto">
                    <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-xl p-8 text-center">
                        <h2 className="text-2xl font-bold mb-4">
                            {contest.status === 'UPCOMING' ? 'Contest Not Started Yet' : 'Contest Has Ended'}
                        </h2>
                        <p className="text-gray-400 mb-6">
                            {contest.status === 'UPCOMING'
                                ? `Contest starts at: ${new Date(contest.startTime).toLocaleString()}`
                                : `Contest ended at: ${new Date(contest.endTime).toLocaleString()}`
                            }
                        </p>
                        <button
                            onClick={() => navigate('/contests/private/my-contests')}
                            className="px-6 py-3 bg-gray-700 hover:bg-gray-600 rounded-lg font-semibold transition-colors"
                        >
                            Back to Contests
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[#131313] text-gray-100">
            {/* Header */}
            <div className="bg-[#1c1b1b] border-b border-gray-800 px-6 py-4">
                <div className="max-w-screen-2xl mx-auto flex items-center justify-between">
                    <div>
                        <h1 className="text-xl font-bold">{contest.name}</h1>
                        <p className="text-sm text-gray-400">
                            Ends: {new Date(contest.endTime).toLocaleString()}
                        </p>
                    </div>
                    <div className="flex items-center gap-4">
                        <button
                            onClick={() => setShowLeaderboard(!showLeaderboard)}
                            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition-colors flex items-center gap-2"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                            </svg>
                            Leaderboard
                        </button>
                        <span className="px-4 py-2 bg-green-500/20 text-green-400 rounded-lg font-semibold">
                            LIVE
                        </span>
                    </div>
                </div>
            </div>

            <div className="flex">
                {/* Problems Sidebar */}
                <div className="w-80 bg-[#1c1b1b] border-r border-gray-800 h-[calc(100vh-73px)] overflow-y-auto">
                    <div className="p-4">
                        <h2 className="text-lg font-bold mb-4">Problems</h2>
                        <div className="space-y-2">
                            {contest.problems && contest.problems.map((problem, index) => (
                                <button
                                    key={problem.id}
                                    onClick={() => setSelectedProblem(problem)}
                                    className={`w-full text-left p-4 rounded-lg transition-all ${selectedProblem?.id === problem.id
                                            ? 'bg-blue-600 text-white'
                                            : 'bg-[#0f0f0f] hover:bg-gray-800 text-gray-300'
                                        }`}
                                >
                                    <div className="flex items-center justify-between mb-2">
                                        <span className="font-semibold">Problem {index + 1}</span>
                                        <span className={`text-xs px-2 py-1 rounded ${problem.level === 'EASY' ? 'bg-green-500/20 text-green-400' :
                                                problem.level === 'MEDIUM' ? 'bg-yellow-500/20 text-yellow-400' :
                                                    'bg-red-500/20 text-red-400'
                                            }`}>
                                            {problem.level}
                                        </span>
                                    </div>
                                    <div className="text-sm">{problem.title}</div>
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Main Content */}
                <div className="flex-1 overflow-y-auto h-[calc(100vh-73px)]">
                    {selectedProblem ? (
                        <div className="p-8">
                            {/* Problem Description */}
                            <div className="mb-8">
                                <div className="flex items-center justify-between mb-4">
                                    <h2 className="text-2xl font-bold">{selectedProblem.title}</h2>
                                    <span className={`px-3 py-1 rounded-lg text-sm font-semibold ${selectedProblem.level === 'EASY' ? 'bg-green-500/20 text-green-400' :
                                            selectedProblem.level === 'MEDIUM' ? 'bg-yellow-500/20 text-yellow-400' :
                                                'bg-red-500/20 text-red-400'
                                        }`}>
                                        {selectedProblem.level}
                                    </span>
                                </div>

                                <div className="bg-[#1c1b1b] rounded-lg p-6 space-y-4">
                                    <div>
                                        <h3 className="font-semibold text-gray-300 mb-2">Description</h3>
                                        <p className="text-gray-400 whitespace-pre-line">{selectedProblem.description}</p>
                                    </div>

                                    {selectedProblem.inputFormat && (
                                        <div>
                                            <h3 className="font-semibold text-gray-300 mb-2">Input Format</h3>
                                            <p className="text-gray-400 whitespace-pre-line">{selectedProblem.inputFormat}</p>
                                        </div>
                                    )}

                                    {selectedProblem.outputFormat && (
                                        <div>
                                            <h3 className="font-semibold text-gray-300 mb-2">Output Format</h3>
                                            <p className="text-gray-400 whitespace-pre-line">{selectedProblem.outputFormat}</p>
                                        </div>
                                    )}

                                    {selectedProblem.constraints && (
                                        <div>
                                            <h3 className="font-semibold text-gray-300 mb-2">Constraints</h3>
                                            <p className="text-gray-400 whitespace-pre-line">{selectedProblem.constraints}</p>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* Code Editor */}
                            <div className="mb-8">
                                <div className="flex items-center justify-between mb-4">
                                    <h3 className="text-lg font-bold">Your Solution</h3>
                                    <select
                                        value={language}
                                        onChange={(e) => setLanguage(e.target.value)}
                                        className="bg-[#1c1b1b] border border-gray-700 rounded-lg px-4 py-2 text-gray-100"
                                    >
                                        <option value="java">Java</option>
                                        <option value="python">Python</option>
                                        <option value="cpp">C++</option>
                                        <option value="c">C</option>
                                        <option value="javascript">JavaScript</option>
                                    </select>
                                </div>

                                <div className="bg-[#1c1b1b] rounded-lg overflow-hidden border border-gray-800">
                                    <Editor
                                        height="400px"
                                        language={language}
                                        value={code}
                                        onChange={(value) => setCode(value || '')}
                                        theme="vs-dark"
                                        options={{
                                            minimap: { enabled: false },
                                            fontSize: 14,
                                            lineNumbers: 'on',
                                            scrollBeyondLastLine: false,
                                            automaticLayout: true,
                                        }}
                                    />
                                </div>
                            </div>

                            {/* Submit Button */}
                            {error && (
                                <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 text-red-400 mb-4">
                                    {error}
                                </div>
                            )}

                            <button
                                onClick={handleSubmit}
                                disabled={submitting || !code.trim()}
                                className="w-full py-3 bg-green-600 hover:bg-green-700 disabled:bg-gray-700 disabled:cursor-not-allowed rounded-lg font-semibold transition-colors"
                            >
                                {submitting ? 'Submitting...' : 'Submit Solution'}
                            </button>
                        </div>
                    ) : (
                        <div className="flex items-center justify-center h-full">
                            <p className="text-gray-400">Select a problem to start solving</p>
                        </div>
                    )}
                </div>

                {/* Leaderboard Sidebar */}
                {showLeaderboard && (
                    <div className="w-96 bg-[#1c1b1b] border-l border-gray-800 h-[calc(100vh-73px)] overflow-y-auto">
                        <div className="p-4">
                            <div className="flex items-center justify-between mb-4">
                                <h2 className="text-lg font-bold">Leaderboard</h2>
                                <button
                                    onClick={() => setShowLeaderboard(false)}
                                    className="text-gray-400 hover:text-gray-300"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            </div>

                            <div className="space-y-2">
                                {leaderboard.map((entry, index) => (
                                    <div
                                        key={entry.userId}
                                        className={`p-4 rounded-lg ${index === 0 ? 'bg-yellow-500/20 border border-yellow-500/30' :
                                                index === 1 ? 'bg-gray-400/20 border border-gray-400/30' :
                                                    index === 2 ? 'bg-orange-500/20 border border-orange-500/30' :
                                                        'bg-[#0f0f0f]'
                                            }`}
                                    >
                                        <div className="flex items-center justify-between mb-2">
                                            <span className="font-bold text-lg">#{entry.rank}</span>
                                            <span className="text-lg font-bold text-blue-400">{entry.totalScore}</span>
                                        </div>
                                        <div className="text-sm text-gray-300">{entry.userName}</div>
                                        <div className="text-xs text-gray-500 mt-1">
                                            {entry.problemsSolved} problems solved
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default PrivateContestArena;
