import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import AuthService from '../services/auth.service';

const Navbar = () => {
    const [currentUser, setCurrentUser] = useState(undefined);
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) {
            setCurrentUser(user);
        }
    }, []);

    const logOut = () => {
        AuthService.logout();
        setCurrentUser(undefined);
        navigate("/");
    };

    const isActive = (path) => location.pathname === path;

    return (
        <motion.nav
            initial={{ y: -100, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.5, ease: "easeOut" }}
            className="bg-black/40 backdrop-blur-xl border-b border-white/10 sticky top-0 z-50 shadow-2xl"
            style={{
                background: 'linear-gradient(180deg, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0.6) 100%)'
            }}
        >
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex items-center justify-between h-16">
                    {/* Logo */}
                    <motion.div
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                    >
                        <Link to="/" className="flex items-center space-x-2">
                            <span className="text-white font-mono text-xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-white to-gray-400">
                                &lt;CodeCombat /&gt;
                            </span>
                        </Link>
                    </motion.div>

                    {/* Navigation Links */}
                    <div className="hidden md:flex items-center space-x-2">
                        {currentUser && currentUser.roles && currentUser.roles.includes('ROLE_ADMIN') && (
                            <NavLink to="/admin/dashboard" isActive={isActive('/admin/dashboard')}>
                                Dashboard
                            </NavLink>
                        )}
                        {currentUser && currentUser.roles && !currentUser.roles.includes('ROLE_ADMIN') && (
                            <NavLink to="/dashboard" isActive={isActive('/dashboard')}>
                                Dashboard
                            </NavLink>
                        )}
                        <NavLink
                            to={currentUser && currentUser.roles && currentUser.roles.includes('ROLE_ADMIN') ? '/admin/contests' : '/contests'}
                            isActive={isActive('/contests') || isActive('/admin/contests')}
                        >
                            Contests
                        </NavLink>
                        {currentUser && currentUser.roles && currentUser.roles.includes('ROLE_ADMIN') && (
                            <NavLink to="/admin/leaderboard" isActive={isActive('/admin/leaderboard')}>
                                Leaderboard
                            </NavLink>
                        )}
                    </div>

                    {/* Auth Section */}
                    <div className="flex items-center space-x-3">
                        {currentUser ? (
                            <motion.div
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                className="hidden md:flex items-center space-x-3"
                            >
                                <div className="px-4 py-2 bg-white/5 backdrop-blur-sm border border-white/10 rounded-lg">
                                    <span className="text-gray-300 text-sm font-medium">{currentUser.username}</span>
                                </div>
                                <motion.button
                                    whileHover={{ scale: 1.05, backgroundColor: 'rgba(255,255,255,0.1)' }}
                                    whileTap={{ scale: 0.95 }}
                                    onClick={logOut}
                                    className="px-4 py-2 bg-white/5 hover:bg-white/10 backdrop-blur-sm border border-white/10 hover:border-white/20 text-gray-300 hover:text-white rounded-lg text-sm font-medium transition-all"
                                >
                                    Logout
                                </motion.button>
                            </motion.div>
                        ) : (
                            <motion.div
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                className="flex items-center space-x-3"
                            >
                                <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                                    <Link
                                        to="/login"
                                        className="px-4 py-2 text-gray-300 hover:text-white rounded-lg text-sm font-medium transition-colors"
                                    >
                                        Login
                                    </Link>
                                </motion.div>
                                <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                                    <Link
                                        to="/register"
                                        className="px-5 py-2 bg-white/10 hover:bg-white/20 backdrop-blur-sm border border-white/20 hover:border-white/30 text-white rounded-lg text-sm font-semibold transition-all shadow-lg"
                                    >
                                        Register
                                    </Link>
                                </motion.div>
                            </motion.div>
                        )}
                    </div>
                </div>
            </div>
        </motion.nav>
    );
};

// NavLink Component with glassmorphism
const NavLink = ({ to, children, isActive }) => {
    return (
        <motion.div
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
        >
            <Link
                to={to}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-all backdrop-blur-sm ${isActive
                    ? 'bg-white/20 text-white border border-white/30 shadow-lg'
                    : 'text-gray-400 hover:text-white hover:bg-white/10 border border-transparent hover:border-white/20'
                    }`}
            >
                {children}
            </Link>
        </motion.div>
    );
};

export default Navbar;
