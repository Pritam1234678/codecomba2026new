import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import AuthService from '../services/auth.service';

const Navbar = () => {
    const [currentUser, setCurrentUser] = useState(undefined);
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
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
        setMobileMenuOpen(false);
        navigate("/");
    };

    const isActive = (path) => location.pathname === path;

    const closeMobileMenu = () => setMobileMenuOpen(false);

    return (
        <>
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

                        {/* Desktop Navigation Links */}
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
                            {currentUser && currentUser.roles && !currentUser.roles.includes('ROLE_ADMIN') && (
                                <NavLink to="/platform-details" isActive={isActive('/platform-details')}>
                                    Platform Details
                                </NavLink>
                            )}
                        </div>

                        {/* Desktop Auth Section */}
                        <div className="hidden md:flex items-center space-x-3">
                            {currentUser ? (
                                <motion.div
                                    initial={{ opacity: 0, x: 20 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    className="flex items-center space-x-3"
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

                        {/* Mobile Hamburger Button */}
                        <motion.button
                            whileTap={{ scale: 0.9 }}
                            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                            className="md:hidden p-2 rounded-lg bg-white/5 border border-white/10 hover:bg-white/10 transition-colors"
                            aria-label="Toggle menu"
                        >
                            <svg
                                className="w-6 h-6 text-white"
                                fill="none"
                                stroke="currentColor"
                                viewBox="0 0 24 24"
                            >
                                {mobileMenuOpen ? (
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                ) : (
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                                )}
                            </svg>
                        </motion.button>
                    </div>
                </div>
            </motion.nav>

            {/* Mobile Menu Overlay */}
            {mobileMenuOpen && (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    onClick={closeMobileMenu}
                    className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 md:hidden"
                />
            )}

            {/* Mobile Slide-out Menu */}
            <motion.div
                initial={{ x: '100%' }}
                animate={{ x: mobileMenuOpen ? 0 : '100%' }}
                transition={{ type: 'spring', damping: 25, stiffness: 200 }}
                className="fixed top-0 right-0 h-full w-72 bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border-l border-white/20 z-50 md:hidden shadow-2xl"
            >
                <div className="flex flex-col h-full p-6 space-y-6">
                    {/* Close Button */}
                    <button
                        onClick={closeMobileMenu}
                        className="self-end p-2 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 hover:border-green-500/50 transition-all duration-300 group"
                    >
                        <svg className="w-6 h-6 text-white group-hover:text-green-400 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>

                    {/* Mobile Navigation Links */}
                    <div className="flex flex-col space-y-3">
                        {currentUser && currentUser.roles && currentUser.roles.includes('ROLE_ADMIN') && (
                            <MobileNavLink to="/admin/dashboard" onClick={closeMobileMenu} isActive={isActive('/admin/dashboard')}>
                                Dashboard
                            </MobileNavLink>
                        )}
                        {currentUser && currentUser.roles && !currentUser.roles.includes('ROLE_ADMIN') && (
                            <MobileNavLink to="/dashboard" onClick={closeMobileMenu} isActive={isActive('/dashboard')}>
                                Dashboard
                            </MobileNavLink>
                        )}
                        <MobileNavLink
                            to={currentUser && currentUser.roles && currentUser.roles.includes('ROLE_ADMIN') ? '/admin/contests' : '/contests'}
                            onClick={closeMobileMenu}
                            isActive={isActive('/contests') || isActive('/admin/contests')}
                        >
                            Contests
                        </MobileNavLink>
                        {currentUser && currentUser.roles && currentUser.roles.includes('ROLE_ADMIN') && (
                            <MobileNavLink to="/admin/leaderboard" onClick={closeMobileMenu} isActive={isActive('/admin/leaderboard')}>
                                Leaderboard
                            </MobileNavLink>
                        )}
                        {currentUser && currentUser.roles && !currentUser.roles.includes('ROLE_ADMIN') && (
                            <MobileNavLink to="/platform-details" onClick={closeMobileMenu} isActive={isActive('/platform-details')}>
                                Platform Details
                            </MobileNavLink>
                        )}
                    </div>

                    {/* Mobile Auth Section */}
                    <div className="flex flex-col space-y-3 mt-auto border-t border-white/10 pt-6">
                        {currentUser ? (
                            <>
                                <div className="px-4 py-3 bg-gradient-to-br from-white/10 to-white/5 backdrop-blur-xl border border-white/20 rounded-2xl text-center shadow-lg">
                                    <span className="text-gray-200 text-sm font-semibold">{currentUser.username}</span>
                                </div>
                                <motion.button
                                    whileHover={{ scale: 1.02 }}
                                    whileTap={{ scale: 0.98 }}
                                    onClick={logOut}
                                    className="w-full px-4 py-3 bg-gradient-to-br from-white/5 to-white/2 hover:from-green-500/20 hover:to-emerald-600/10 backdrop-blur-xl border border-white/10 hover:border-green-500/50 text-gray-300 hover:text-white rounded-2xl text-sm font-semibold transition-all duration-300 shadow-lg hover:shadow-green-500/20"
                                >
                                    Logout
                                </motion.button>
                            </>
                        ) : (
                            <>
                                <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                                    <Link
                                        to="/login"
                                        onClick={closeMobileMenu}
                                        className="w-full block px-4 py-3 text-center bg-gradient-to-br from-white/5 to-white/2 hover:from-green-500/10 hover:to-emerald-600/5 backdrop-blur-xl border border-white/10 hover:border-green-500/50 text-gray-300 hover:text-white rounded-2xl text-sm font-semibold transition-all duration-300 shadow-lg hover:shadow-green-500/20"
                                    >
                                        Login
                                    </Link>
                                </motion.div>
                                <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                                    <Link
                                        to="/register"
                                        onClick={closeMobileMenu}
                                        className="w-full block px-5 py-3 text-center bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 backdrop-blur-xl border border-green-500/30 hover:border-green-400/50 text-white rounded-2xl text-sm font-bold transition-all duration-300 shadow-lg shadow-green-500/30 hover:shadow-green-500/50"
                                    >
                                        Register
                                    </Link>
                                </motion.div>
                            </>
                        )}
                    </div>
                </div>
            </motion.div>
        </>
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

// MobileNavLink Component for mobile menu
const MobileNavLink = ({ to, children, isActive, onClick }) => {
    return (
        <motion.div
            whileHover={{ scale: 1.02, x: 4 }}
            whileTap={{ scale: 0.98 }}
        >
            <Link
                to={to}
                onClick={onClick}
                className={`block w-full px-4 py-3 rounded-2xl text-sm font-semibold transition-all duration-300 ${isActive
                    ? 'bg-gradient-to-r from-green-500/20 to-emerald-600/20 text-white border border-green-500/50 shadow-lg shadow-green-500/20'
                    : 'bg-gradient-to-br from-white/5 to-white/2 text-gray-400 hover:text-white hover:from-green-500/10 hover:to-emerald-600/10 border border-white/10 hover:border-green-500/30 hover:shadow-lg hover:shadow-green-500/10'
                    }`}
            >
                {children}
            </Link>
        </motion.div>
    );
};

export default Navbar;
