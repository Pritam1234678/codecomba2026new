import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import api from '../services/api';
import ProblemService from '../services/problem.service';

const ProblemSolve = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [problem, setProblem] = useState(null);
    const [code, setCode] = useState("// Write your code here\n");
    const [language, setLanguage] = useState("JAVA");
    const [output, setOutput] = useState(null);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [running, setRunning] = useState(false);
    const [hasExistingSubmission, setHasExistingSubmission] = useState(false);
    const [snippets, setSnippets] = useState({});
    const [snippetsLoaded, setSnippetsLoaded] = useState(false);
    const [isProblemCollapsed, setIsProblemCollapsed] = useState(false);
    const [isConsoleCollapsed, setIsConsoleCollapsed] = useState(false);
    const [allProblems, setAllProblems] = useState([]);
    const [currentIndex, setCurrentIndex] = useState(-1);

    // Contest status monitoring
    const [contestStatus, setContestStatus] = useState({
        active: true,
        exists: true,
        contestName: '',
        endTime: null,
        checked: false
    });
    const [showStatusBanner, setShowStatusBanner] = useState(false);
    const [timeRemaining, setTimeRemaining] = useState('');

    // Fetch problem details and existing submission
    useEffect(() => {
        // Reset all states when problem changes
        setLoading(true);
        setHasExistingSubmission(false);
        setOutput(null);
        setCode("// Write your code here\n");
        setLanguage("JAVA");
        setSnippets({});
        setSnippetsLoaded(false);

        // Fetch problem details
        api.get(`/problems/${id}`)
            .then(res => {
                setProblem(res.data);
                setLoading(false);
            })
            .catch(err => {
                console.error('Problem fetch error:', err);
                setLoading(false);
            });

        // Fetch code snippets
        ProblemService.getSnippets(id)
            .then(res => {
                const snippetMap = {};
                res.data.forEach(snippet => {
                    snippetMap[snippet.language] = snippet.starterCode;
                });
                setSnippets(snippetMap);
                setSnippetsLoaded(true);

                // Fetch existing submission after snippets are loaded
                api.get(`/submissions/user/${id}`)
                    .then(subRes => {
                        if (subRes.data) {
                            // Found existing submission
                            console.log('Loaded existing submission:', subRes.data);
                            setHasExistingSubmission(true);
                            setCode(subRes.data.code);
                            setLanguage(subRes.data.language);
                            setOutput(
                                <div className="space-y-2">
                                    <div className="bg-gradient-to-r from-emerald-500 to-teal-600 text-white text-sm font-bold px-4 py-2 rounded-lg shadow-md flex items-center gap-2">
                                        <span className="text-lg">✓</span>
                                        <span>Already Submitted</span>
                                    </div>
                                    <p className="font-mono text-xs text-gray-300">Loaded your last submission (Status: <span className="text-emerald-400 font-bold">{subRes.data.status}</span>)</p>
                                    <p className="font-mono text-xs text-yellow-400 bg-yellow-500/10 px-3 py-2 rounded border border-yellow-500/30">⚠️ If you submit again, your original code will be replaced by your new submission</p>
                                </div>
                            );
                        } else {
                            // No submission, load starter code from snippets
                            const defaultLang = "JAVA";
                            if (snippetMap[defaultLang]) {
                                setCode(snippetMap[defaultLang]);
                                setLanguage(defaultLang);
                            }
                        }
                    })
                    .catch(err => {
                        // 404 means no submission exists - this is expected for new problems
                        if (err.response?.status !== 404) {
                            console.error('Unexpected error fetching submission:', err);
                        }
                        // Load starter code from snippets
                        const defaultLang = "JAVA";
                        if (snippetMap[defaultLang]) {
                            setCode(snippetMap[defaultLang]);
                            setLanguage(defaultLang);
                        }
                    });
            })
            .catch(err => {
                console.error('Snippet fetch error:', err);
                setSnippetsLoaded(true);
            });
    }, [id]);

    // Fetch all problems for navigation
    useEffect(() => {
        api.get('/problems')
            .then(res => {
                const problems = res.data;
                console.log('All problems fetched:', problems);
                setAllProblems(problems);

                // Find current problem index
                const index = problems.findIndex(p => p.id === parseInt(id));
                console.log('Current problem ID:', id, 'Index:', index, 'Total problems:', problems.length);
                setCurrentIndex(index);
            })
            .catch(err => {
                console.error('Error fetching problems:', err);
            });
    }, [id]);

    // Poll contest status every 10 seconds
    useEffect(() => {
        const checkContestStatus = async () => {
            try {
                const res = await api.get(`/problems/${id}/contest-status`);
                const status = res.data;

                setContestStatus({
                    ...status,
                    checked: true
                });

                // If contest was deleted, problem is also deleted (cascade)
                // Show "Problem Not Found" page
                if (!status.exists) {
                    setProblem(null);
                    return;
                }

                // If contest is just deactivated, show banner
                if (!status.active) {
                    setShowStatusBanner(true);
                }
            } catch (err) {
                console.error('Error checking contest status:', err);
                // If API fails, problem might be deleted
                if (err.response?.status === 404) {
                    setProblem(null);
                }
            }
        };

        // Initial check
        checkContestStatus();

        // Poll every 10 seconds
        const interval = setInterval(() => {
            checkContestStatus();
        }, 10000);

        // Cleanup on unmount
        return () => clearInterval(interval);
    }, [id]);

    // Countdown timer - updates every second
    useEffect(() => {
        if (!contestStatus.endTime) {
            setTimeRemaining('');
            return;
        }

        const calculateTimeLeft = () => {
            const now = new Date();
            const end = new Date(contestStatus.endTime);
            const diff = end - now;

            if (diff <= 0) {
                setTimeRemaining('Ended');
                return;
            }

            const hours = Math.floor(diff / (1000 * 60 * 60));
            const minutes = Math.floor((diff / (1000 * 60)) % 60);
            const seconds = Math.floor((diff / 1000) % 60);

            if (hours > 0) {
                setTimeRemaining(`${hours}h ${minutes}m remaining`);
            } else if (minutes > 0) {
                setTimeRemaining(`${minutes}m ${seconds}s remaining`);
            } else {
                setTimeRemaining(`${seconds}s remaining`);
            }
        };

        calculateTimeLeft();
        const timer = setInterval(calculateTimeLeft, 1000);

        return () => clearInterval(timer);
    }, [contestStatus.endTime]);


    // Handle manual language change (only update if no submission or explicit change)
    const handleLanguageChange = (newLang) => {
        setLanguage(newLang);

        // Load snippet if available
        if (snippets[newLang]) {
            setCode(snippets[newLang]);
        } else {
            setCode(''); // Empty if no snippet available
        }
    };

    const handleRun = () => {
        // Check contest status before running
        if (!contestStatus.active || !contestStatus.exists) {
            setShowStatusBanner(true);
            // Redirect to contests page after 2 seconds
            setTimeout(() => navigate('/contests'), 2000);
            return;
        }

        setRunning(true);
        setOutput("Running tests...");

        api.post('/submissions/test', {
            problemId: id,
            code: code,
            language: language
        }).then(res => {
            setRunning(false);
            const sub = res.data;

            // Determine status color
            let statusColor = 'text-red-500';
            if (sub.status === 'AC') statusColor = 'text-green-500';
            else if (sub.status === 'JUDGING' || sub.status === 'PENDING') statusColor = 'text-yellow-500';

            // Check for compilation or runtime errors
            if (sub.status === 'CE' || sub.status === 'RE') {
                setOutput(
                    <div className="font-mono text-sm">
                        <p className={`text-xl font-bold mb-2 ${statusColor}`}>
                            {sub.status === 'CE' ? 'Compilation Error' : 'Runtime Error'}
                        </p>
                        <div className="bg-code-gray p-3 rounded mt-2 text-red-400 whitespace-pre-wrap overflow-x-auto">
                            {sub.errorMessage || 'No error details available'}
                        </div>
                        <p className="text-yellow-500 mt-2 text-xs">💡 This was a test run - not saved</p>
                    </div>
                );
            } else {
                // Normal verdict display (no time for test runs)
                const testCaseDetails = sub.testCaseDetails ? JSON.parse(sub.testCaseDetails) : [];
                const visibleTestCases = testCaseDetails.filter(tc => !tc.hidden);

                setOutput(
                    <div className={`font-mono text-sm ${statusColor}`}>
                        <p className="text-xl font-bold mb-2">{sub.status}</p>
                        <div className="text-gray-300">
                            <p>Passed: {sub.testCasesPassed} / {sub.totalTestCases}</p>

                            {/* Test Case Details - Only show non-hidden test cases */}
                            {visibleTestCases.length > 0 && (
                                <div className="mt-3 border-t border-gray-700 pt-2">
                                    <p className="text-xs text-gray-400 mb-1">Test Case Results:</p>
                                    <div className="space-y-1">
                                        {visibleTestCases.map((tc, idx) => (
                                            <div key={idx} className="flex items-center gap-2 text-xs">
                                                <span className="text-gray-500">TC #{tc.testCase}:</span>
                                                <span className={tc.status === 'PASS' ? 'text-green-400' : 'text-red-400'}>
                                                    {tc.status === 'PASS' ? '✓ PASS' : '✗ FAIL'}
                                                </span>
                                            </div>
                                        ))}
                                        {testCaseDetails.length > visibleTestCases.length && (
                                            <p className="text-xs text-gray-500 italic mt-1">
                                                + {testCaseDetails.length - visibleTestCases.length} hidden test case(s)
                                            </p>
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>
                        <p className="text-yellow-500 mt-2 text-xs">💡 This was a test run - not saved</p>
                    </div>
                );
            }
        }).catch(err => {
            setRunning(false);
            setOutput(<div className="text-red-500">Test failed: {err.message}</div>);
        });
    };

    // Navigation handlers
    const handlePreviousProblem = () => {
        // Check contest status before navigating
        if (!contestStatus.active || !contestStatus.exists) {
            setShowStatusBanner(true);
            setTimeout(() => navigate('/contests'), 2000);
            return;
        }

        console.log('Previous clicked! currentIndex:', currentIndex, 'allProblems.length:', allProblems.length);
        if (currentIndex > 0 && allProblems.length > 0) {
            const prevProblem = allProblems[currentIndex - 1];
            console.log('Navigating to previous problem:', prevProblem);
            navigate(`/problems/${prevProblem.id}`);
        } else {
            console.log('Cannot navigate to previous - at first problem or no problems loaded');
        }
    };

    const handleNextProblem = () => {
        // Check contest status before navigating
        if (!contestStatus.active || !contestStatus.exists) {
            setShowStatusBanner(true);
            setTimeout(() => navigate('/contests'), 2000);
            return;
        }

        console.log('Next clicked! currentIndex:', currentIndex, 'allProblems.length:', allProblems.length);
        if (currentIndex < allProblems.length - 1 && allProblems.length > 0) {
            const nextProblem = allProblems[currentIndex + 1];
            console.log('Navigating to next problem:', nextProblem);
            navigate(`/problems/${nextProblem.id}`);
        } else {
            console.log('Cannot navigate to next - at last problem or no problems loaded');
        }
    };

    const handleSubmit = () => {
        // Check contest status before submitting
        if (!contestStatus.active || !contestStatus.exists) {
            setShowStatusBanner(true);
            // Redirect to contests page after 2 seconds
            setTimeout(() => navigate('/contests'), 2000);
            return;
        }

        setSubmitting(true);
        setOutput("Running tests...");

        api.post('/submissions', {
            problemId: id,
            code: code,
            language: language
        }).then(res => {
            setSubmitting(false);
            const sub = res.data;

            // Determine status color
            let statusColor = 'text-red-500';
            if (sub.status === 'AC') statusColor = 'text-green-500';
            else if (sub.status === 'JUDGING' || sub.status === 'PENDING') statusColor = 'text-yellow-500';

            // Check for compilation or runtime errors
            if (sub.status === 'CE' || sub.status === 'RE') {
                setOutput(
                    <div className="font-mono text-sm">
                        <p className={`text-xl font-bold mb-2 ${statusColor}`}>
                            {sub.status === 'CE' ? 'Compilation Error' : 'Runtime Error'}
                        </p>
                        <div className="bg-code-gray p-3 rounded mt-2 text-red-400 whitespace-pre-wrap overflow-x-auto">
                            {sub.errorMessage || 'No error details available'}
                        </div>
                    </div>
                );
            } else {
                // Normal verdict display with time
                const testCaseDetails = sub.testCaseDetails ? JSON.parse(sub.testCaseDetails) : [];
                const visibleTestCases = testCaseDetails.filter(tc => !tc.hidden);

                setOutput(
                    <div className={`font-mono text-sm ${statusColor}`}>
                        <p className="text-xl font-bold mb-2">{sub.status}</p>
                        <div className="text-gray-300">
                            <p>Passed: {sub.testCasesPassed} / {sub.totalTestCases}</p>
                            {sub.timeConsumed > 0 && <p>Time: {sub.timeConsumed}ms</p>}
                            {/* <p className="text-code-green font-bold mt-1">Score: {sub.score || 0}/100</p> */}

                            {/* Test Case Details - Only show non-hidden test cases */}
                            {visibleTestCases.length > 0 && (
                                <div className="mt-3 border-t border-gray-700 pt-2">
                                    <p className="text-xs text-gray-400 mb-1">Test Case Results:</p>
                                    <div className="space-y-1">
                                        {visibleTestCases.map((tc, idx) => (
                                            <div key={idx} className="flex items-center gap-2 text-xs">
                                                <span className="text-gray-500">TC #{tc.testCase}:</span>
                                                <span className={tc.status === 'PASS' ? 'text-green-400' : 'text-red-400'}>
                                                    {tc.status === 'PASS' ? '✓ PASS' : '✗ FAIL'}
                                                </span>
                                            </div>
                                        ))}
                                        {testCaseDetails.length > visibleTestCases.length && (
                                            <p className="text-xs text-gray-500 italic mt-1">
                                                + {testCaseDetails.length - visibleTestCases.length} hidden test case(s)
                                            </p>
                                        )}
                                    </div>
                                </div>
                            )}
                        </div>
                        <p className="text-blue-400 mt-2 text-xs">✓ Submission saved (updates previous if exists)</p>
                    </div>
                );
            }
        }).catch(err => {
            setSubmitting(false);
            setOutput(<div className="text-red-500">Submission failed: {err.message}</div>);
        });
    };

    if (loading) return <div className="text-center mt-20 text-code-green animate-pulse">Loading Arena...</div>;

    if (!problem) {
        return (
            <div className="max-w-7xl mx-auto px-4 py-8 flex items-center justify-center min-h-[80vh]">
                <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-12 shadow-2xl max-w-2xl w-full text-center">
                    <div className="mb-6">
                        <span className="text-8xl">🔍</span>
                    </div>
                    <h1 className="text-4xl font-bold bg-gradient-to-r from-red-400 via-red-500 to-red-600 bg-clip-text text-transparent mb-4">
                        Problem Not Found
                    </h1>
                    <p className="text-xl text-gray-400 mb-2">
                        The problem you're looking for doesn't exist or has been removed.
                    </p>
                    <p className="text-sm text-gray-500 mb-8">
                        Problem ID: <span className="font-mono text-red-400">{id}</span>
                    </p>
                    <div className="flex gap-4 justify-center">
                        <button
                            onClick={() => navigate('/contests')}
                            className="px-6 py-3 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold rounded-xl shadow-lg transition-all transform hover:scale-105"
                        >
                            Browse Contests
                        </button>
                        <button
                            onClick={() => navigate('/dashboard')}
                            className="px-6 py-3 bg-white/10 hover:bg-white/20 text-white font-semibold rounded-xl border border-white/30 transition-all"
                        >
                            Go to Dashboard
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="h-[calc(100vh-5rem)] flex flex-col md:flex-row gap-4 overflow-hidden relative">
            {/* Contest Status Banner */}
            {showStatusBanner && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
                    <div className="bg-gradient-to-br from-red-600 to-red-700 border-2 border-red-400 rounded-2xl p-8 shadow-2xl max-w-3xl w-full mx-4 transform scale-100 animate-pulse">
                        <div className="flex items-start justify-between">
                            <div className="flex-1">
                                <h2 className="text-4xl font-bold text-white mb-4 flex items-center gap-3">
                                    <span className="text-5xl">⚠️</span>
                                    {!contestStatus.exists
                                        ? 'Contest Deleted!'
                                        : 'Contest Deactivated!'}
                                </h2>
                                <p className="text-xl text-white/95 mb-6 leading-relaxed">
                                    {!contestStatus.exists
                                        ? 'This contest has been removed by the administrator. Your submissions will not be accepted.'
                                        : `The contest "${contestStatus.contestName}" has been deactivated by the administrator. Submissions are no longer allowed.`}
                                </p>
                                <button
                                    onClick={() => navigate('/contests')}
                                    className="px-6 py-3 bg-white text-red-600 rounded-xl text-lg font-bold hover:bg-gray-100 transition-all transform hover:scale-105 shadow-lg"
                                >
                                    Go to Contests →
                                </button>
                            </div>
                            <button
                                onClick={() => setShowStatusBanner(false)}
                                className="text-white/80 hover:text-white ml-6 text-2xl font-bold"
                            >
                                ✕
                            </button>
                        </div>
                    </div>
                </div>
            )}


            {/* Left Pane: Problem Description */}
            <div className={`transition-all duration-300 ease-in-out bg-code-black border border-code-gray rounded-lg overflow-hidden ${isProblemCollapsed ? 'w-12' : 'w-full md:w-1/3'
                }`}>
                {/* Collapse Header */}
                <div className="flex items-center justify-between bg-code-dark border-b border-code-gray p-3">
                    {!isProblemCollapsed && (
                        <h2 className="text-sm font-semibold text-gray-300 uppercase tracking-wide">Problem</h2>
                    )}
                    <button
                        onClick={() => setIsProblemCollapsed(!isProblemCollapsed)}
                        className="p-1 hover:bg-white/10 rounded transition-colors text-gray-400 hover:text-white ml-auto"
                        title={isProblemCollapsed ? 'Expand Problem' : 'Collapse Problem'}
                    >
                        {isProblemCollapsed ? (
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                            </svg>
                        ) : (
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                            </svg>
                        )}
                    </button>
                </div>

                {/* Collapsed Vertical Text */}
                {isProblemCollapsed && (
                    <div className="flex items-center justify-center h-full">
                        <span className="text-gray-400 text-xs font-semibold tracking-wider" style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}>
                            PROBLEM
                        </span>
                    </div>
                )}

                {/* Problem Content */}
                {!isProblemCollapsed && (
                    <div className="p-6 overflow-y-auto scrollbar-thin" style={{ maxHeight: 'calc(100vh - 8rem)' }}>
                        <h1 className="text-2xl font-bold text-white mb-4">{problem.title}</h1>
                        <div className="prose prose-invert max-w-none text-gray-300 mb-6 whitespace-pre-wrap">
                            {problem.description}
                        </div>

                        <h3 className="text-lg font-bold text-white mt-6 mb-2">Input Format</h3>
                        <pre className="bg-code-gray p-3 rounded text-sm text-gray-300 whitespace-pre-wrap">{problem.inputFormat}</pre>

                        <h3 className="text-lg font-bold text-white mt-6 mb-2">Output Format</h3>
                        <pre className="bg-code-gray p-3 rounded text-sm text-gray-300 whitespace-pre-wrap">{problem.outputFormat}</pre>
                        {/* Example 1 - Only if exists */}
                        {problem.example1 && (
                            <div className="mt-6">
                                <h3 className="text-lg font-bold text-white mb-2">Example 1:</h3>
                                <div className="bg-code-gray p-4 rounded border border-gray-700">
                                    <pre className="text-gray-300 whitespace-pre-wrap font-mono text-sm">
                                        {problem.example1}
                                    </pre>
                                </div>
                            </div>
                        )}

                        {/* Example 2 - Only if exists */}
                        {problem.example2 && (
                            <div className="mt-6">
                                <h3 className="text-lg font-bold text-white mb-2">Example 2:</h3>
                                <div className="bg-code-gray p-4 rounded border border-gray-700">
                                    <pre className="text-gray-300 whitespace-pre-wrap font-mono text-sm">
                                        {problem.example2}
                                    </pre>
                                </div>
                            </div>
                        )}

                        {/* Example 3 - Only if exists */}
                        {problem.example3 && (
                            <div className="mt-6">
                                <h3 className="text-lg font-bold text-white mb-2">Example 3:</h3>
                                <div className="bg-code-gray p-4 rounded border border-gray-700">
                                    <pre className="text-gray-300 whitespace-pre-wrap font-mono text-sm">
                                        {problem.example3}
                                    </pre>
                                </div>
                            </div>
                        )}


                        <h3 className="text-lg font-bold text-white mt-6 mb-2">Constraints</h3>
                        <pre className="bg-code-gray p-3 rounded text-sm text-gray-300 whitespace-pre-wrap">{problem.constraints}</pre>


                        {/* Images - Only if exists */}
                        {problem.images && (
                            <div className="mt-6">
                                <h3 className="text-lg font-bold text-white mb-2">Images:</h3>
                                <div className="grid grid-cols-1 gap-4">
                                    {problem.images.split(',').map((url, index) => {
                                        const trimmedUrl = url.trim();
                                        // Convert Google Drive share links to direct image URLs
                                        let imageUrl = trimmedUrl;
                                        const driveMatch = trimmedUrl.match(/\/file\/d\/([^\/]+)/);
                                        if (driveMatch) {
                                            imageUrl = `https://drive.google.com/uc?export=view&id=${driveMatch[1]}`;
                                        }

                                        return (
                                            <img
                                                key={index}
                                                src={imageUrl}
                                                alt={`Example ${index + 1}`}
                                                className="rounded-lg border border-gray-700 w-full min-h-[400px] object-contain"
                                                onError={(e) => {
                                                    e.target.onerror = null;
                                                    e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="400" height="300" fill="%23374151"/><text x="50%" y="50%" text-anchor="middle" fill="%239CA3AF" font-family="monospace">Image failed to load</text></svg>';
                                                }}
                                            />
                                        );
                                    })}
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Right Pane: Editor */}
            <div className={`transition-all duration-300 ease-in-out flex flex-col gap-4 ${isProblemCollapsed ? 'w-full md:w-[calc(100%-4rem)]' : 'w-full md:w-2/3'
                }`}>
                <div className="bg-code-black border border-code-gray rounded-lg flex-1 flex flex-col overflow-hidden">
                    <div className="bg-code-dark border-b border-code-gray p-3 flex justify-between items-center">
                        <select
                            className="bg-code-gray text-white border-none rounded px-3 py-1 text-sm focus:ring-1 focus:ring-code-green"
                            value={language}
                            onChange={(e) => handleLanguageChange(e.target.value)}
                        >
                            <option value="JAVA">Java</option>
                            <option value="PYTHON">Python</option>
                            <option value="CPP">C++</option>
                            <option value="C">C</option>
                            <option value="JAVASCRIPT">JavaScript</option>
                        </select>

                        <div className="flex gap-3 items-center">
                            {/* Countdown Timer - Next to language selector */}
                            {timeRemaining && contestStatus.endTime && (
                                <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl px-3 py-1.5">
                                    <div className="flex items-center gap-1.5">
                                        <svg className="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                        <span className="text-gray-300 font-medium text-xs whitespace-nowrap">{timeRemaining}</span>
                                    </div>
                                </div>
                            )}

                            {hasExistingSubmission && (
                                <div className="bg-linear-to-r from-emerald-500 to-teal-600 text-white text-[10px] font-bold px-3 py-1 rounded shadow-sm cursor-default flex items-center gap-1 uppercase tracking-wider">
                                    <span>✓ Already Submitted</span>
                                </div>
                            )}

                            {/* Navigation Buttons */}
                            <div className="flex gap-2">
                                <button
                                    onClick={handlePreviousProblem}
                                    disabled={currentIndex < 0 || currentIndex === 0 || allProblems.length === 0}
                                    className="relative bg-white/10 hover:bg-white/20 backdrop-blur-sm border border-white/20 hover:border-white/30 text-white font-bold px-6 py-1.5 rounded text-sm transition-all disabled:opacity-30 disabled:cursor-not-allowed flex items-center gap-2 shadow-lg"
                                    title="Previous Problem"
                                >
                                    <span>← Previous</span>
                                </button>
                                <button
                                    onClick={handleNextProblem}
                                    disabled={currentIndex < 0 || currentIndex >= allProblems.length - 1 || allProblems.length === 0}
                                    className="relative bg-white/10 hover:bg-white/20 backdrop-blur-sm border border-white/20 hover:border-white/30 text-white font-bold px-6 py-1.5 rounded text-sm transition-all disabled:opacity-30 disabled:cursor-not-allowed flex items-center gap-2 shadow-lg"
                                    title="Next Problem"
                                >
                                    <span>Next →</span>
                                </button>
                            </div>

                            <div className="flex gap-2">
                                <button
                                    onClick={handleRun}
                                    disabled={running || submitting}
                                    className="relative bg-gradient-to-br from-blue-500 to-blue-700 text-white font-bold px-6 py-1.5 rounded text-sm hover:from-blue-400 hover:to-blue-600 transition-all disabled:opacity-50 shadow-lg overflow-hidden before:absolute before:inset-0 before:bg-gradient-to-br before:from-white/20 before:to-transparent before:opacity-0 hover:before:opacity-100 before:transition-opacity"
                                >
                                    {running ? 'Running...' : '▶ Run'}
                                </button>
                                <button
                                    onClick={handleSubmit}
                                    disabled={submitting || running}
                                    className="relative bg-gradient-to-br from-green-500 to-green-700 text-white font-bold px-6 py-1.5 rounded text-sm hover:from-green-400 hover:to-green-600 transition-all disabled:opacity-50 shadow-lg overflow-hidden before:absolute before:inset-0 before:bg-gradient-to-br before:from-white/20 before:to-transparent before:opacity-0 hover:before:opacity-100 before:transition-opacity"
                                >
                                    {submitting ? 'Submitting...' : '✓ Submit'}
                                </button>
                            </div>

                        </div>
                    </div>

                    <Editor
                        height="100%"
                        theme="vs-dark"
                        language={language === 'JAVA' ? 'java' : language === 'CPP' ? 'cpp' : language === 'C' ? 'c' : language === 'PYTHON' ? 'python' : 'javascript'}
                        value={code}
                        onChange={(value) => setCode(value)}
                        options={{
                            // Visual Enhancements
                            minimap: { enabled: true, scale: 1, showSlider: 'mouseover' },
                            fontSize: 15,
                            fontFamily: "'Fira Code', 'Cascadia Code', 'JetBrains Mono', 'Consolas', monospace",
                            fontLigatures: true,
                            lineHeight: 22,
                            letterSpacing: 0.5,
                            scrollBeyondLastLine: false,
                            automaticLayout: true,
                            smoothScrolling: true,
                            cursorBlinking: 'smooth',
                            cursorSmoothCaretAnimation: true,

                            // Code Folding & Structure
                            folding: true,
                            foldingStrategy: 'indentation',
                            showFoldingControls: 'always',

                            // Bracket Matching & Colorization
                            matchBrackets: 'always',
                            bracketPairColorization: {
                                enabled: true,
                                independentColorPoolPerBracketType: true
                            },
                            guides: {
                                bracketPairs: true,
                                indentation: false,
                                highlightActiveIndentation: false
                            },

                            // Multi-cursor & Selection
                            multiCursorModifier: 'ctrlCmd',
                            selectionHighlight: true,
                            occurrencesHighlight: true,
                            renderLineHighlight: 'all',
                            renderWhitespace: 'selection',

                            // IntelliSense & Autocomplete
                            suggestOnTriggerCharacters: true,
                            quickSuggestions: {
                                other: true,
                                comments: true,
                                strings: true
                            },
                            parameterHints: {
                                enabled: true,
                                cycle: true
                            },
                            suggest: {
                                showKeywords: true,
                                showSnippets: true,
                                showClasses: true,
                                showFunctions: true,
                                showVariables: true,
                                showModules: true,
                                showProperties: true,
                                showMethods: true,
                                showConstructors: true,
                                showFields: true,
                                showInterfaces: true,
                                showEnums: true,
                                showConstants: true,
                                showStructs: true,
                                showEvents: true,
                                showOperators: true,
                                showTypeParameters: true,
                                insertMode: 'replace',
                                snippetsPreventQuickSuggestions: false
                            },
                            acceptSuggestionOnCommitCharacter: true,
                            acceptSuggestionOnEnter: "on",
                            tabCompletion: "on",
                            wordBasedSuggestions: true,

                            // Code Formatting
                            formatOnPaste: true,
                            formatOnType: true,
                            autoClosingBrackets: "always",
                            autoClosingQuotes: "always",
                            autoClosingOvertype: "always",
                            autoIndent: "full",
                            autoSurround: "languageDefined",

                            // Find & Replace
                            find: {
                                addExtraSpaceOnTop: true,
                                autoFindInSelection: 'multiline',
                                seedSearchStringFromSelection: 'selection'
                            },

                            // Scrollbars
                            scrollbar: {
                                vertical: 'visible',
                                horizontal: 'visible',
                                useShadows: true,
                                verticalHasArrows: false,
                                horizontalHasArrows: false,
                                verticalScrollbarSize: 14,
                                horizontalScrollbarSize: 14
                            },

                            // Line Numbers & Gutter
                            lineNumbers: 'on',
                            lineNumbersMinChars: 3,
                            glyphMargin: true,

                            // Hover & Links
                            hover: {
                                enabled: true,
                                delay: 300,
                                sticky: true
                            },
                            links: true,

                            // Performance
                            renderValidationDecorations: 'on',
                            codeLens: false,

                            // Accessibility
                            accessibilitySupport: 'auto',

                            // Padding
                            padding: {
                                top: 16,
                                bottom: 16
                            },

                            // ========== ADDITIONAL ADVANCED FEATURES ==========
                            // Sticky Scroll (VS Code-like)
                            stickyScroll: {
                                enabled: true,
                                maxLineCount: 5
                            },

                            // Mouse Features
                            mouseWheelZoom: true,
                            fastScrollSensitivity: 5,
                            mouseWheelScrollSensitivity: 1,

                            // Cursor Enhancements
                            cursorStyle: 'line',
                            cursorWidth: 2,

                            // Minimap Enhancements
                            minimap: {
                                enabled: true,
                                scale: 1,
                                showSlider: 'mouseover',
                                renderCharacters: true,
                                maxColumn: 120,
                                side: 'right'
                            },

                            // Inline Suggestions (Copilot-like)
                            inlineSuggest: {
                                enabled: true,
                                mode: 'prefix'
                            },

                            // Advanced Suggestions (merged with existing)
                            suggest: {
                                showKeywords: true,
                                showSnippets: true,
                                showClasses: true,
                                showFunctions: true,
                                showVariables: true,
                                showModules: true,
                                showProperties: true,
                                showMethods: true,
                                showConstructors: true,
                                showFields: true,
                                showInterfaces: true,
                                showEnums: true,
                                showConstants: true,
                                showStructs: true,
                                showEvents: true,
                                showOperators: true,
                                showTypeParameters: true,
                                insertMode: 'replace',
                                snippetsPreventQuickSuggestions: false,
                                showWords: true,
                                showColors: true,
                                showFiles: true,
                                showReferences: true,
                                showFolders: true,
                                showUnits: true,
                                showValues: true,
                                filterGraceful: true,
                                localityBonus: true,
                                shareSuggestSelections: true,
                                showInlineDetails: true,
                                showStatusBar: true
                            },

                            // Word-based Suggestions
                            wordBasedSuggestionsMode: 'matchingDocuments',
                            quickSuggestionsDelay: 10,

                            // Folding Enhancements
                            foldingHighlight: true,
                            unfoldOnClickAfterEndOfLine: true,

                            // Bracket Guides (merged with existing)
                            guides: {
                                bracketPairs: true,
                                indentation: true,
                                highlightActiveIndentation: true,
                                bracketPairsHorizontal: 'active',
                                highlightActiveBracketPair: true
                            },

                            // Selection & Highlighting
                            multiCursorMergeOverlapping: true,
                            multiCursorPaste: 'spread',
                            renderLineHighlightOnlyWhenFocus: false,
                            highlightActiveIndentGuide: true,

                            // Special Characters
                            renderControlCharacters: true,
                            renderFinalNewline: true,

                            // Editing Features
                            emptySelectionClipboard: true,
                            copyWithSyntaxHighlighting: true,
                            dragAndDrop: true,
                            dropIntoEditor: {
                                enabled: true
                            },

                            // Comments
                            comments: {
                                insertSpace: true,
                                ignoreEmptyLines: true
                            },

                            // Auto-closing
                            autoClosingDelete: "always",

                            // Find Enhancements (merged with existing)
                            find: {
                                addExtraSpaceOnTop: true,
                                autoFindInSelection: 'multiline',
                                seedSearchStringFromSelection: 'selection',
                                globalFindClipboard: false,
                                loop: true
                            },

                            // Scrollbar Enhancements (merged with existing)
                            scrollbar: {
                                vertical: 'visible',
                                horizontal: 'visible',
                                useShadows: true,
                                verticalHasArrows: false,
                                horizontalHasArrows: false,
                                verticalScrollbarSize: 14,
                                horizontalScrollbarSize: 14,
                                arrowSize: 11,
                                handleMouseWheel: true,
                                alwaysConsumeMouseWheel: true,
                                scrollByPage: false
                            },

                            // Gutter
                            lineDecorationsWidth: 10,

                            // Hover Enhancements (merged with existing)
                            hover: {
                                enabled: true,
                                delay: 300,
                                sticky: true,
                                above: true
                            },

                            // Semantic Features
                            'semanticHighlighting.enabled': true,
                            colorDecorators: true,

                            // Diff Editor
                            diffWordWrap: 'on',

                            // Accessibility
                            accessibilityPageSize: 10,

                            // Rulers (vertical lines)
                            rulers: [80, 120],

                            // Word Wrap
                            wordWrap: 'off',
                            wordWrapColumn: 120,
                            wrappingIndent: 'indent',
                            wrappingStrategy: 'advanced',

                            // Snippets
                            snippetSuggestions: 'top',

                            // Tab Settings
                            tabSize: 4,
                            insertSpaces: true,
                            detectIndentation: true,
                            trimAutoWhitespace: true,

                            // Selection
                            selectOnLineNumbers: true,
                            selectionClipboard: true,
                            roundedSelection: true,

                            // Overview Ruler
                            overviewRulerBorder: true,
                            overviewRulerLanes: 3,

                            // Context Menu
                            contextmenu: true,

                            // Experimental
                            experimentalWhitespaceRendering: 'svg',

                            // Unicode Highlighting
                            unicodeHighlight: {
                                ambiguousCharacters: true,
                                invisibleCharacters: true,
                                nonBasicASCII: true
                            },

                            // Font Weight
                            fontWeight: '400'
                        }}
                    />
                </div>

                {/* Output Console */}
                <div className={`transition-all duration-300 ease-in-out bg-code-black border border-code-gray rounded-lg overflow-hidden ${isConsoleCollapsed ? 'h-12' : 'h-1/3'
                    }`}>
                    {/* Console Header */}
                    <div className="flex items-center justify-between bg-code-dark border-b border-code-gray p-3">
                        <h3 className="text-gray-500 text-xs font-bold uppercase tracking-widest">Verdict Console</h3>
                        <button
                            onClick={() => setIsConsoleCollapsed(!isConsoleCollapsed)}
                            className="p-1 hover:bg-white/10 rounded transition-colors text-gray-400 hover:text-white"
                            title={isConsoleCollapsed ? 'Expand Console' : 'Collapse Console'}
                        >
                            {isConsoleCollapsed ? (
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                                </svg>
                            ) : (
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
                                </svg>
                            )}
                        </button>
                    </div>

                    {/* Console Content */}
                    {!isConsoleCollapsed && (
                        <div className="p-4 overflow-y-auto font-mono text-sm" style={{ maxHeight: 'calc(33vh - 3rem)' }}>
                            {output ? output : <span className="text-gray-600 italic">Run your code to see results...</span>}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ProblemSolve;
