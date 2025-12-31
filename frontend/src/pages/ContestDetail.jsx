import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';
import api from '../services/api';

// Countdown Timer Component
const CountdownTimer = ({ endTime }) => {
  const [timeLeft, setTimeLeft] = useState('');

  useEffect(() => {
    const calculateTimeLeft = () => {
      const now = new Date();
      const end = new Date(endTime);
      const diff = end - now;

      if (diff <= 0) {
        return 'Ended';
      }

      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
      const minutes = Math.floor((diff / (1000 * 60)) % 60);
      const seconds = Math.floor((diff / 1000) % 60);

      if (days > 0) {
        return `${days}d ${hours}h remaining`;
      } else if (hours > 0) {
        return `${hours}h ${minutes}m remaining`;
      } else {
        return `${minutes}m ${seconds}s remaining`;
      }
    };

    setTimeLeft(calculateTimeLeft());
    const timer = setInterval(calculateTimeLeft, 1000);

    return () => clearInterval(timer);
  }, [endTime]);

  return (
    <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-xl px-4 py-2">
      <div className="flex items-center gap-2">
        <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span className="text-gray-300 font-medium text-sm whitespace-nowrap">{timeLeft}</span>
      </div>
    </div>
  );
};

const ContestDetail = () => {
  const { id } = useParams();
  const [contest, setContest] = useState(null);
  const [problems, setProblems] = useState([]);
  const [submissions, setSubmissions] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const headerRef = useRef(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const contestRes = await api.get(`/contests/${id}`);
        const problemsRes = await api.get(`/problems/contest/${id}`);
        setContest(contestRes.data);
        setProblems(problemsRes.data);

        try {
          const submissionsRes = await api.get('/submissions/user');
          const submissionsMap = {};
          submissionsRes.data.forEach(sub => {
            const problemId = sub.problemId;
            if (problemId && (!submissionsMap[problemId] || new Date(sub.submittedAt) > new Date(submissionsMap[problemId].submittedAt))) {
              submissionsMap[problemId] = sub;
            }
          });
          setSubmissions(submissionsMap);
        } catch (err) {
          console.log('No submissions found or error fetching submissions');
        }

        setLoading(false);
      } catch (err) {
        console.error(err);
        // Check if contest was deleted (404) or other error
        if (err.response?.status === 404) {
          setError('Contest not found');
        } else {
          setError('Failed to load contest');
        }
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  useEffect(() => {
    if (!loading && headerRef.current) {
      gsap.from(headerRef.current, {
        opacity: 0,
        y: 30,
        duration: 0.8,
        ease: 'power3.out'
      });
    }
  }, [loading]);

  const getStatusColor = (status) => {
    switch (status) {
      case 'AC': return 'text-green-400 bg-green-500/10 border-green-500/30';
      case 'WA': return 'text-red-400 bg-red-500/10 border-red-500/30';
      case 'TLE': return 'text-yellow-400 bg-yellow-500/10 border-yellow-500/30';
      case 'RE': return 'text-orange-400 bg-orange-500/10 border-orange-500/30';
      case 'CE': return 'text-red-400 bg-red-400/10 border-red-400/30';
      case 'PENDING':
      case 'JUDGING': return 'text-blue-400 bg-blue-500/10 border-blue-500/30';
      default: return 'text-gray-400 bg-gray-500/10 border-gray-500/30';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400 text-lg">Loading...</div>
      </div>
    );
  }

  if (!contest || error) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-red-500/30 rounded-3xl p-8 shadow-2xl text-center">
          <h1 className="text-2xl font-semibold text-red-400 mb-4">{error || 'Contest not found'}</h1>
          <p className="text-gray-400 mb-6">The contest you are looking for does not exist or has been removed.</p>
          <Link
            to="/contests"
            className="inline-block px-6 py-3 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105"
          >
            ← Back to Contests
          </Link>
        </div>
      </div>
    );
  }

  // Check if contest is disabled
  if (!contest.active) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-red-500/30 rounded-3xl p-8 shadow-2xl text-center">
          <h1 className="text-2xl font-semibold text-red-400 mb-4">Contest Not Available</h1>
          <p className="text-gray-400 mb-6">This contest has been disabled by the administrator.</p>
          <Link
            to="/contests"
            className="inline-block px-6 py-3 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105"
          >
            ← Back to Contests
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8 space-y-4 sm:space-y-6">
      {/* Header */}
      <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 lg:p-8 shadow-2xl">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-start gap-3 mb-3">
          <h1 className="text-xl sm:text-2xl lg:text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent">{contest.name}</h1>
          {/* Countdown Timer - Right side of heading */}
          {contest.status === 'LIVE' && contest.endTime && (
            <CountdownTimer endTime={contest.endTime} />
          )}
        </div>
        <p className="text-xs sm:text-sm text-gray-400 mb-4 sm:mb-6">{contest.description}</p>
        <div className="flex flex-col sm:flex-row gap-2 sm:gap-4 text-xs text-gray-500">
          <div className="bg-white/10 px-3 py-1.5 rounded">Start: {new Date(contest.startTime).toLocaleString()}</div>
          <div className="bg-white/10 px-3 py-1.5 rounded">End: {new Date(contest.endTime).toLocaleString()}</div>
        </div>
      </div>

      {/* Problems */}
      <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 shadow-2xl">
        <h2 className="text-lg sm:text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-4 sm:mb-6">Problems</h2>
        <div className="space-y-4">
          {problems.length === 0 ? (
            <div className="text-center py-8 text-gray-500">No problems released yet</div>
          ) : (
            problems.map((problem, index) => {
              const submission = submissions[problem.id];
              const hasSubmission = !!submission;

              return (
                <motion.div
                  key={problem.id}
                  whileHover={{ y: -2 }}
                  transition={{ duration: 0.2, ease: 'easeOut' }}
                  className="bg-white/5 border border-white/10 p-4 sm:p-6 rounded-xl hover:border-green-500/30 transition-all"
                >
                  <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4">
                    <div className="flex-1">
                      <div className="flex flex-wrap items-center gap-2 sm:gap-3 mb-2">
                        <h3 className="text-base sm:text-lg font-semibold text-gray-200">
                          {String.fromCharCode(65 + index)}. {problem.title}
                        </h3>
                        {hasSubmission && (
                          <span className={`text-xs font-medium px-2.5 py-1 rounded border ${getStatusColor(submission.status)}`}>
                            {submission.status}
                          </span>
                        )}
                      </div>
                      <div className="flex flex-wrap gap-2 sm:gap-4 text-xs text-gray-500">
                        <span>Time: {problem.timeLimit}s</span>
                        <span>Memory: {problem.memoryLimit}MB</span>
                      </div>
                    </div>
                    <Link
                      to={`/problems/${problem.id}`}
                      className={`w-full sm:w-auto text-center font-medium px-6 py-2.5 sm:py-2 rounded-xl transition-all ${hasSubmission
                        ? 'bg-blue-500/10 hover:bg-blue-500/20 border border-blue-500/30 hover:border-blue-500/50 text-blue-400'
                        : 'bg-green-500/10 hover:bg-green-500/20 border border-green-500/30 hover:border-green-500/50 text-green-400'
                        }`}
                    >
                      {hasSubmission ? 'View →' : 'Solve →'}
                    </Link>
                  </div>
                </motion.div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};

export default ContestDetail;
