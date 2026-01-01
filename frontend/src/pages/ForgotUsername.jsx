import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

const ForgotUsername = () => {
    const [formData, setFormData] = useState({
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
            const response = await api.post('/auth/forgot-username', formData);
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
                        <span className="text-green-400">Username?</span>
                    </h2>
                    <p className="text-gray-400 text-sm">
                        We'll send your username to your registered email.
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
                                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                                        <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                    </svg>
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
                                    'Send Username'
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

export default ForgotUsername;
