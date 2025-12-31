import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';
import api from '../services/api';

const Leaderboard = () => {
  const navigate = useNavigate();
  const [contests, setContests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const headerRef = useRef(null);

  useEffect(() => {
    fetchContests();
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

  const fetchContests = async () => {
    try {
      const res = await api.get('/contests');
      setContests(res.data);
      setLoading(false);
    } catch (err) {
      console.error('Error fetching contests:', err);
      setError('Failed to load contests');
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

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8 space-y-4 sm:space-y-6">
      <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 lg:p-8 shadow-2xl">
        <h1 className="text-xl sm:text-2xl lg:text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-1">
          Leaderboard
        </h1>
        <p className="text-xs sm:text-sm text-gray-500">Select a contest to view rankings</p>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 text-red-400">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {contests.length === 0 ? (
          <div className="col-span-full bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-6 sm:p-8 lg:p-12 text-center shadow-xl">
            <p className="text-gray-500">No contests available</p>
          </div>
        ) : (
          contests.map((contest) => (
            <motion.div
              key={contest.id}
              whileHover={{ y: -2, scale: 1.01 }}
              transition={{ duration: 0.2, ease: 'easeOut' }}
              className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 hover:border-white/30 transition-all shadow-xl hover:shadow-2xl flex flex-col"
            >
              <div className="flex-1">
                <h3 className="text-lg sm:text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-3">{contest.name}</h3>
                <p className="text-sm text-gray-400 mb-4 min-h-[40px]">{contest.description || 'No description'}</p>

                <div className="space-y-1 text-xs text-gray-500 mb-6">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-400">Start:</span>
                    <span>{new Date(contest.startTime).toLocaleDateString()}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-gray-400">End:</span>
                    <span>{new Date(contest.endTime).toLocaleDateString()}</span>
                  </div>
                </div>
              </div>

              <button
                onClick={() => navigate(`/admin/leaderboard/${contest.id}`)}
                className="w-full bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold py-3 px-4 rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105"
              >
                Show Leaderboard →
              </button>
            </motion.div>
          ))
        )}
      </div>
    </div>
  );
};

export default Leaderboard;
