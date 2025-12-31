import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import AuthService from '../services/auth.service';

const Home = () => {
    const [currentUser, setCurrentUser] = useState(null);
    const [isAdmin, setIsAdmin] = useState(false);

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) {
            setCurrentUser(user);
            setIsAdmin(user.roles && user.roles.includes('ROLE_ADMIN'));
        }
    }, []);

    // Dynamic button configuration based on user role
    const getButtonConfig = () => {
        if (!currentUser) {
            // Not logged in
            return {
                primary: { text: "Enter Arena →", link: "/login" },
                secondary: { text: "Get Started", link: "/login" },
                cta: { text: "Start Coding Now →", link: "/register" }
            };
        } else if (isAdmin) {
            // Admin user
            return {
                primary: { text: "Modify Contest →", link: "/admin/contests" },
                secondary: { text: "Get Started", link: "/admin/dashboard" },
                cta: { text: "Show Leaderboard →", link: "/admin/leaderboard" }
            };
        } else {
            // Normal user
            return {
                primary: { text: "Enter Arena →", link: "/contests" },
                secondary: { text: "Get Started", link: "/dashboard" },
                cta: { text: "Start Coding Now →", link: "/contests" }
            };
        }
    };

    const buttons = getButtonConfig();

    return (
        <div className="relative w-full">
            {/* Hero Section - Full Screen */}
            <section className="relative min-h-screen flex items-center justify-center px-4">
                <div className="max-w-6xl mx-auto text-center">
                    {/* Animated Headline */}
                    <motion.h1
                        initial={{ opacity: 0, y: 30 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.8, ease: "easeOut" }}
                        className="text-6xl md:text-8xl font-bold mb-8 leading-tight"
                    >
                        <span className="inline-block mr-4" style={{ color: '#e5e5e5', textShadow: '0 0 10px rgba(255,255,255,0.2)' }}>&lt;</span>
                        <span className="inline-block mr-4 text-green-400" style={{ textShadow: '0 0 15px rgba(74,222,128,0.3)' }}>
                            CodeCombat
                        </span>
                        <span className="inline-block" style={{ color: '#e5e5e5', textShadow: '0 0 10px rgba(255,255,255,0.2)' }}>/&gt;</span>
                    </motion.h1>

                    {/* Subtext */}
                    <motion.p
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.3, ease: "easeOut" }}
                        className="text-2xl md:text-3xl text-gray-300 mb-12 max-w-4xl mx-auto font-light"
                        style={{ textShadow: '0 0 5px rgba(255,255,255,0.1)' }}
                    >
                        Master competitive programming with real-time code execution
                    </motion.p>

                    {/* CTA Buttons */}
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, delay: 0.6, ease: "easeOut" }}
                        className="flex flex-col sm:flex-row gap-6 justify-center"
                    >
                        <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                            <Link
                                to={buttons.primary.link}
                                className="inline-block px-10 py-5 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white text-lg font-bold rounded-xl shadow-2xl shadow-green-500/50 transition-all"
                            >
                                {buttons.primary.text}
                            </Link>
                        </motion.div>
                        <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                            <Link
                                to={buttons.secondary.link}
                                className="inline-block px-10 py-5 bg-white/10 hover:bg-white/20 backdrop-blur-md border-2 border-white/30 hover:border-white/50 text-white text-lg font-semibold rounded-xl transition-all"
                            >
                                {buttons.secondary.text}
                            </Link>
                        </motion.div>
                    </motion.div>

                    {/* Scroll Indicator */}
                    <motion.div
                        animate={{ y: [0, 10, 0] }}
                        transition={{ repeat: Infinity, duration: 2 }}
                        className="mt-20"
                    >
                        <div className="text-gray-500 text-sm">Scroll to explore</div>
                        <div className="mt-2 text-2xl text-gray-500">↓</div>
                    </motion.div>
                </div>
            </section>

            {/* Features Section */}
            <section className="relative py-32 px-4 bg-gradient-to-b from-transparent via-black/50 to-black">
                <div className="max-w-7xl mx-auto">
                    <motion.h2
                        initial={{ opacity: 0, y: 30 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true }}
                        transition={{ duration: 0.8 }}
                        className="text-5xl md:text-6xl font-bold text-center mb-20 bg-clip-text text-transparent bg-gradient-to-r from-white to-gray-400"
                    >
                        Why CodeCombat?
                    </motion.h2>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                        {[
                            {
                                icon: "⚡",
                                title: "Lightning Fast",
                                description: "Docker-based execution with instant feedback on every submission"
                            },
                            {
                                icon: "🏆",
                                title: "Compete & Win",
                                description: "Join live contests and climb the leaderboard with your skills"
                            },
                            {
                                icon: "📈",
                                title: "Track Progress",
                                description: "Monitor your growth with detailed analytics and insights"
                            }
                        ].map((feature, index) => (
                            <motion.div
                                key={index}
                                initial={{ opacity: 0, y: 40 }}
                                whileInView={{ opacity: 1, y: 0 }}
                                viewport={{ once: true }}
                                transition={{ duration: 0.6, delay: index * 0.2 }}
                                whileHover={{ y: -8, scale: 1.02 }}
                                className="group relative bg-gradient-to-br from-white/5 to-white/[0.02] backdrop-blur-xl border border-white/10 rounded-2xl p-8 hover:border-green-500/50 transition-all duration-300"
                            >
                                <div className="text-6xl mb-6 group-hover:scale-110 transition-transform duration-300">
                                    {feature.icon}
                                </div>
                                <h3 className="text-2xl font-bold mb-4 text-white">{feature.title}</h3>
                                <p className="text-gray-400 text-lg leading-relaxed">{feature.description}</p>

                                {/* Glow effect on hover */}
                                <div className="absolute inset-0 bg-gradient-to-r from-green-500/0 via-green-500/5 to-green-500/0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 rounded-2xl" />
                            </motion.div>
                        ))}
                    </div>
                </div>
            </section>

            {/* Stats Section */}
            <section className="relative py-32 px-4 bg-black">
                <div className="max-w-6xl mx-auto">
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-12">
                        {[
                            { value: "500+", label: "Problems" },
                            { value: "10K+", label: "Users" },
                            { value: "100+", label: "Contests" },
                            { value: "99.9%", label: "Uptime" }
                        ].map((stat, index) => (
                            <motion.div
                                key={index}
                                initial={{ opacity: 0, scale: 0.5 }}
                                whileInView={{ opacity: 1, scale: 1 }}
                                viewport={{ once: true }}
                                transition={{ duration: 0.5, delay: index * 0.1 }}
                                className="text-center"
                            >
                                <div className="text-5xl md:text-7xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-green-400 to-emerald-500 mb-3">
                                    {stat.value}
                                </div>
                                <div className="text-gray-400 text-lg uppercase tracking-widest">
                                    {stat.label}
                                </div>
                            </motion.div>
                        ))}
                    </div>
                </div>
            </section>

            {/* Final CTA */}
            <section className="relative py-32 px-4 bg-gradient-to-t from-black via-green-950/20 to-black">
                <motion.div
                    initial={{ opacity: 0, y: 40 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.8 }}
                    className="max-w-4xl mx-auto text-center"
                >
                    <h2 className="text-5xl md:text-7xl font-bold mb-8 text-white">
                        Ready to <span className="bg-clip-text text-transparent bg-gradient-to-r from-green-400 to-emerald-500">dominate</span>?
                    </h2>
                    <p className="text-2xl text-gray-300 mb-12 font-light">
                        Join the arena and prove your coding prowess
                    </p>
                    <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                        <Link
                            to={buttons.cta.link}
                            className="inline-block px-12 py-6 bg-linear-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white text-xl font-bold rounded-xl shadow-2xl shadow-green-500/50 transition-all"
                        >
                            {buttons.cta.text}
                        </Link>
                    </motion.div>
                </motion.div>
            </section>
        </div>
    );
};

export default Home;
