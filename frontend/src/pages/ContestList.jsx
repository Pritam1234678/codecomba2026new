import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
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
    const timer = setInterval(() => {
      setTimeLeft(calculateTimeLeft());
    }, 1000);

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


const ContestList = () => {
  const [contests, setContests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const headerRef = useRef(null);

  useEffect(() => {
    api.get('/contests')
      .then(response => {
        setContests(response.data);
        setLoading(false);
      })
      .catch(err => {
        setError("Failed to load contests.");
        setLoading(false);
      });
  }, []);

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

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-gray-400 text-lg">Loading...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-red-400 text-lg">{error}</div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-8 shadow-2xl">
        <h1 className="text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-1">
          Contest Arena
        </h1>
        <p className="text-sm text-gray-500">Choose a contest and start solving problems</p>
      </div>

      {/* Contests Grid */}
      <div className="space-y-4">
        {contests.length === 0 ? (
          <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-12 text-center shadow-xl">
            <p className="text-gray-500">No active contests found</p>
          </div>
        ) : (
          contests.map((contest, index) => (
            <motion.div
              key={contest.id}
              whileHover={{ y: -2, scale: 1.01 }}
              transition={{ duration: 0.2, ease: 'easeOut' }}
              className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 hover:border-white/30 transition-all shadow-xl hover:shadow-2xl"
            >
              <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h2 className="text-2xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent">{contest.name}</h2>
                    <span className={`px-2.5 py-1 rounded text-xs font-medium ${contest.status === 'LIVE' ? 'bg-green-500/20 text-green-400 animate-pulse' :
                      contest.status === 'UPCOMING' ? 'bg-yellow-500/20 text-yellow-400' :
                        'bg-gray-500/20 text-gray-400'
                      }`}>
                      {contest.status}
                    </span>
                  </div>
                  <div className="flex gap-4 text-xs text-gray-500 mb-3">
                    <span>Start: {new Date(contest.startTime).toLocaleString()}</span>
                    <span>End: {new Date(contest.endTime).toLocaleString()}</span>
                  </div>
                  <p className="text-sm text-gray-400">{contest.description}</p>
                </div>
              </div>

              <div className="flex justify-end items-center gap-3 pt-4 border-t border-white/10">
                {/* Countdown Timer - Right side, button-sized */}
                {contest.status === 'LIVE' && contest.endTime && (
                  <CountdownTimer endTime={contest.endTime} />
                )}

                <Link
                  to={`/contests/${contest.id}`}
                  className="px-6 py-2 rounded-xl text-sm font-semibold bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white shadow-lg shadow-green-500/30 transition-all transform hover:scale-105"
                >
                  Enter Arena →
                </Link>
              </div>
            </motion.div>
          ))
        )}
      </div>
    </div>
  );
};

export default ContestList;

