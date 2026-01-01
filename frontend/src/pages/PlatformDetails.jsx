import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';

const PlatformDetails = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const headerRef = useRef(null);
    const overviewRef = useRef(null);
    const featuresRef = useRef(null);
    const techStackRef = useRef(null);
    const statsRef = useRef(null);
    const systemRef = useRef(null);

    const platformInfo = {
        name: 'CodeCombat 2026',
        version: '1.0.0',
        description: 'A competitive programming platform for coding contests and problem solving',
        features: [
            'Real-time code execution',
            'Multiple programming languages support',
            'Contest management system',
            'Leaderboard and rankings',
            'Problem submission and evaluation',
            'User profile management'
        ],
        techStack: {
            frontend: ['React', 'Vite', 'TailwindCSS', 'Framer Motion', 'GSAP'],
            backend: ['Spring Boot', 'Java', 'MySQL', 'Spring Security', 'JWT'],
            deployment: ['Docker', 'Nginx', 'AWS']
        },
        statistics: {
            totalProblems: '50+',
            supportedLanguages: 'C, C++, Java, Python',
            maxSubmissionSize: '5MB',
            executionTimeout: '10 seconds'
        }
    };

    useEffect(() => {
        // Simulate loading
        const timer = setTimeout(() => {
            setLoading(false);
        }, 800);
        return () => clearTimeout(timer);
    }, []);

    useEffect(() => {
        if (!loading) {
            // GSAP animations - slide from left to center
            const elements = [
                headerRef.current,
                overviewRef.current,
                featuresRef.current,
                techStackRef.current,
                statsRef.current,
                systemRef.current
            ].filter(Boolean);

            gsap.fromTo(
                elements,
                {
                    x: -100,
                    opacity: 0
                },
                {
                    x: 0,
                    opacity: 1,
                    duration: 0.8,
                    stagger: 0.15,
                    ease: 'power3.out'
                }
            );
        }
    }, [loading]);

    if (loading) {
        return (
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8 space-y-4 sm:space-y-6">
                {/* Header Skeleton */}
                <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 lg:p-8 shadow-2xl animate-pulse">
                    <div className="h-8 bg-white/10 rounded-lg w-1/3 mb-2"></div>
                    <div className="h-4 bg-white/5 rounded w-1/2"></div>
                </div>

                {/* Content Skeleton */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
                    {[1, 2, 3, 4].map((i) => (
                        <div key={i} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl p-4 sm:p-6 animate-pulse">
                            <div className="h-6 bg-white/10 rounded w-1/2 mb-4"></div>
                            <div className="space-y-2">
                                <div className="h-4 bg-white/5 rounded"></div>
                                <div className="h-4 bg-white/5 rounded w-5/6"></div>
                                <div className="h-4 bg-white/5 rounded w-4/6"></div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    return (
        <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-6 lg:py-8 space-y-4 sm:space-y-6"
        >
            {/* Header */}
            <div ref={headerRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 lg:p-8 shadow-2xl">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                    <div>
                        <h1 className="text-xl sm:text-2xl lg:text-3xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-2">
                            Platform Details
                        </h1>
                        <p className="text-gray-500 text-xs sm:text-sm">Complete information about the platform</p>
                    </div>
                    <button
                        onClick={() => navigate('/admin/dashboard')}
                        className="w-full sm:w-auto px-4 py-2 bg-white/5 hover:bg-white/10 border border-white/20 hover:border-white/30 text-gray-300 rounded-xl transition-all text-center"
                    >
                        ← Back to Dashboard
                    </button>
                </div>
            </div>

            {/* Platform Overview */}
            <div ref={overviewRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 shadow-2xl">
                <h2 className="text-lg sm:text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-4">Platform Overview</h2>
                <div className="space-y-4">
                    <div>
                        <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Platform Name</div>
                        <div className="text-lg font-semibold text-gray-200">{platformInfo.name}</div>
                    </div>
                    <div>
                        <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Version</div>
                        <div className="text-gray-300">{platformInfo.version}</div>
                    </div>
                    <div>
                        <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-1">Description</div>
                        <div className="text-gray-300">{platformInfo.description}</div>
                    </div>
                </div>
            </div>

            {/* Features */}
            <div ref={featuresRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-2xl sm:rounded-3xl p-4 sm:p-6 shadow-2xl">
                <h2 className="text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-4">Key Features</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {platformInfo.features.map((feature, index) => (
                        <div key={index} className="flex items-center gap-3 p-3 bg-white/5 rounded-lg border border-green-500/10">
                            <svg className="w-5 h-5 text-green-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                            </svg>
                            <span className="text-gray-300">{feature}</span>
                        </div>
                    ))}
                </div>
            </div>

            {/* Tech Stack */}
            <div ref={techStackRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 shadow-2xl">
                <h2 className="text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-4">Technology Stack</h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div>
                        <div className="text-sm font-semibold text-gray-400 mb-3">Frontend</div>
                        <div className="space-y-2">
                            {platformInfo.techStack.frontend.map((tech, index) => (
                                <div key={index} className="px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-gray-300 text-sm">
                                    {tech}
                                </div>
                            ))}
                        </div>
                    </div>
                    <div>
                        <div className="text-sm font-semibold text-gray-400 mb-3">Backend</div>
                        <div className="space-y-2">
                            {platformInfo.techStack.backend.map((tech, index) => (
                                <div key={index} className="px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-gray-300 text-sm">
                                    {tech}
                                </div>
                            ))}
                        </div>
                    </div>
                    <div>
                        <div className="text-sm font-semibold text-gray-400 mb-3">Deployment</div>
                        <div className="space-y-2">
                            {platformInfo.techStack.deployment.map((tech, index) => (
                                <div key={index} className="px-3 py-2 bg-white/5 border border-white/10 rounded-lg text-gray-300 text-sm">
                                    {tech}
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            {/* Platform Statistics */}
            <div ref={statsRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 shadow-2xl">
                <h2 className="text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-4">Platform Statistics</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                    <div className="p-4 bg-white/5 rounded-xl border border-white/10">
                        <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Total Problems</div>
                        <div className="text-2xl font-bold text-green-400">{platformInfo.statistics.totalProblems}</div>
                    </div>
                    <div className="p-4 bg-white/5 rounded-xl border border-white/10">
                        <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Supported Languages</div>
                        <div className="text-sm font-semibold text-gray-300">{platformInfo.statistics.supportedLanguages}</div>
                    </div>
                    <div className="p-4 bg-white/5 rounded-xl border border-white/10">
                        <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Max Submission Size</div>
                        <div className="text-sm font-semibold text-gray-300">{platformInfo.statistics.maxSubmissionSize}</div>
                    </div>
                    <div className="p-4 bg-white/5 rounded-xl border border-white/10">
                        <div className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Execution Timeout</div>
                        <div className="text-sm font-semibold text-gray-300">{platformInfo.statistics.executionTimeout}</div>
                    </div>
                </div>
            </div>

            {/* System Information */}
            <div ref={systemRef} className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-white/20 rounded-3xl p-6 shadow-2xl">
                <h2 className="text-xl font-semibold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-4">System Information</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="p-4 bg-white/5 rounded-xl border border-white/10">
                        <div className="flex items-center gap-3 mb-2">
                            <svg className="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <div className="text-sm font-semibold text-gray-400">Server Status</div>
                        </div>
                        <div className="text-green-400 font-medium">Online & Operational</div>
                    </div>
                    <div className="p-4 bg-white/5 rounded-xl border border-white/10">
                        <div className="flex items-center gap-3 mb-2">
                            <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
                            </svg>
                            <div className="text-sm font-semibold text-gray-400">Database</div>
                        </div>
                        <div className="text-gray-300 font-medium">MySQL 8.0</div>
                    </div>
                    <div className="p-4 bg-white/5 rounded-xl border border-white/10">
                        <div className="flex items-center gap-3 mb-2">
                            <svg className="w-5 h-5 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                            </svg>
                            <div className="text-sm font-semibold text-gray-400">Security</div>
                        </div>
                        <div className="text-gray-300 font-medium">JWT + Spring Security</div>
                    </div>
                    <div className="p-4 bg-white/5 rounded-xl border border-white/10">
                        <div className="flex items-center gap-3 mb-2">
                            <svg className="w-5 h-5 text-yellow-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                            </svg>
                            <div className="text-sm font-semibold text-gray-400">Performance</div>
                        </div>
                        <div className="text-gray-300 font-medium">Optimized</div>
                    </div>
                </div>
            </div>
        </motion.div>
    );
};

export default PlatformDetails;
