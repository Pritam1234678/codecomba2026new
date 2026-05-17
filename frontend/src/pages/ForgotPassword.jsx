import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

const ForgotPassword = () => {
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        phoneNumber: ''
    });
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [success, setSuccess] = useState(false);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage('');
        setLoading(true);

        try {
            const response = await api.post('/auth/forgot-password', formData);
            setSuccess(true);
            setMessage(response.data.message);
        } catch (error) {
            setSuccess(false);
            const errorMessage = error.response?.data?.message || 'An error occurred. Please try again.';
            setMessage(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center px-4 py-12">
            <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.5 }}
                className="bg-gradient-to-br from-white/5 to-white/[0.02] backdrop-blur-xl border border-white/10 p-8 md:p-12 rounded-2xl shadow-2xl w-full max-w-md"
            >
                {/* Header */}
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.6, delay: 0.2 }}
                    className="text-center mb-8"
                >
                    <h2 className="text-4xl font-bold mb-2">
                        <span className="text-white">Forgot </span>
                        <span className="text-green-400">Password?</span>
                    </h2>
                    <p className="text-gray-400 text-sm">
                        Don't worry, user. We'll help you reset your password.
                    </p>
                </motion.div>

                {/* Message */}
                {message && (
                    <motion.div
                        initial={{ opacity: 0, y: -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className={`border px-4 py-3 rounded-xl mb-6 ${success
                            ? 'bg-green-500/10 border-green-500/50'
                            : 'bg-red-500/10 border-red-500/50'
                            }`}
                    >
                        <p className={`text-sm ${success ? 'text-green-300' : 'text-red-300'}`}>
                            {message}
                        </p>
                        {success && (
                            <div className="mt-3 pt-3 border-t border-green-500/30">
                                <p className="text-yellow-400 text-sm font-semibold flex items-center gap-2">
                                  
                                    If you don't see this email in your inbox, please check your spam/junk folder.
                                </p>
                            </div>
                        )}
                    </motion.div>
                )}

                {!success && (
                    <motion.form
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ duration: 0.6, delay: 0.3 }}
                        onSubmit={handleSubmit}
                        className="space-y-5"
                    >
                        {/* Username */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Username *
                            </label>
                            <input
                                name="username"
                                type="text"
                                value={formData.username}
                                onChange={handleChange}
                                required
                                className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                                placeholder="Enter your username"
                            />
                        </div>

                        {/* Email */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Registered Email *
                            </label>
                            <input
                                name="email"
                                type="email"
                                value={formData.email}
                                onChange={handleChange}
                                required
                                className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                                placeholder="your.email@example.com"
                            />
                        </div>

                        {/* Phone Number */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Registered Phone Number *
                            </label>
                            <input
                                name="phoneNumber"
                                type="text"
                                value={formData.phoneNumber}
                                onChange={handleChange}
                                required
                                className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                                placeholder="+91 1234567890"
                            />
                        </div>

                        {/* Submit Button */}
                        <div className="pt-2">
                            <motion.button
                                whileHover={{ scale: 1.02 }}
                                whileTap={{ scale: 0.98 }}
                                type="submit"
                                disabled={loading}
                                className="w-full bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-bold py-3 px-4 rounded-xl shadow-lg shadow-green-500/30 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {loading ? (
                                    <span className="flex items-center justify-center gap-2">
                                        <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                        </svg>
                                        Sending...
                                    </span>
                                ) : (
                                    'Send Reset Link'
                                )}
                            </motion.button>
                        </div>
                    </motion.form>
                )}

                {/* Back to Login */}
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ duration: 0.6, delay: 0.5 }}
                    className="mt-6 text-center"
                >
                    <Link
                        to="/login"
                        className="text-green-400 hover:text-green-300 text-sm font-semibold transition-colors"
                    >
                        ← Back to Login
                    </Link>
                </motion.div>
            </motion.div>
        </div>
    );
};

export default ForgotPassword;
