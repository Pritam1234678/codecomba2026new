import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

const ResetPassword = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get('token');

    const [formData, setFormData] = useState({
        newPassword: '',
        confirmPassword: ''
    });
    const [loading, setLoading] = useState(false);
    const [validating, setValidating] = useState(true);
    const [tokenValid, setTokenValid] = useState(false);
    const [fullName, setFullName] = useState('user');
    const [message, setMessage] = useState('');
    const [success, setSuccess] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [passwordError, setPasswordError] = useState('');

    useEffect(() => {
        validateToken();
    }, [token]);

    const validateToken = async () => {
        if (!token) {
            setMessage('Invalid password reset link.');
            setValidating(false);
            return;
        }

        try {
            const response = await api.get(`/auth/validate-reset-token/${token}`);
            setTokenValid(true);
            setFullName(response.data.fullName);
        } catch (error) {
            setTokenValid(false);
            setMessage(error.response?.data?.message || 'This password reset link is invalid or has expired.');
        } finally {
            setValidating(false);
        }
    };

    const validatePassword = (password) => {
        if (password.length < 8) {
            return 'Password must be at least 8 characters long';
        }
        if (!/[A-Z]/.test(password)) {
            return 'Password must contain at least one uppercase letter';
        }
        if (!/[a-z]/.test(password)) {
            return 'Password must contain at least one lowercase letter';
        }
        if (!/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
            return 'Password must contain at least one special character';
        }
        return '';
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });

        // Real-time password validation
        if (name === 'newPassword') {
            const error = validatePassword(value);
            setPasswordError(error);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage('');

        // Validate password
        const passwordValidationError = validatePassword(formData.newPassword);
        if (passwordValidationError) {
            setMessage(passwordValidationError);
            return;
        }

        if (formData.newPassword !== formData.confirmPassword) {
            setMessage('Passwords do not match.');
            return;
        }

        setLoading(true);

        try {
            const response = await api.post('/auth/reset-password', {
                token,
                newPassword: formData.newPassword
            });
            setSuccess(true);
            setMessage(response.data.message);
            setTimeout(() => navigate('/login'), 3000);
        } catch (error) {
            setSuccess(false);
            setMessage(error.response?.data?.message || 'An error occurred. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    if (validating) {
        return (
            <div className="min-h-screen flex items-center justify-center px-4">
                <div className="text-center">
                    <svg className="animate-spin h-12 w-12 text-green-500 mx-auto mb-4" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    <p className="text-gray-400">Validating reset link...</p>
                </div>
            </div>
        );
    }

    if (!tokenValid) {
        return (
            <div className="min-h-screen flex items-center justify-center px-4">
                <motion.div
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="bg-gradient-to-br from-white/5 to-white/[0.02] backdrop-blur-xl border border-white/10 p-8 rounded-2xl shadow-2xl max-w-md text-center"
                >
                    <div className="text-red-400 text-5xl mb-4">⚠️</div>
                    <h2 className="text-2xl font-bold text-white mb-4">Invalid Link</h2>
                    <p className="text-gray-400 mb-6">{message}</p>
                    <Link
                        to="/forgot-password"
                        className="inline-block bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-bold py-3 px-6 rounded-xl transition-all"
                    >
                        Request New Link
                    </Link>
                </motion.div>
            </div>
        );
    }

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
                        <span className="text-white">Reset </span>
                        <span className="text-green-400">Password</span>
                    </h2>
                    <p className="text-gray-400 text-sm">
                        Don't worry, {fullName}. We'll help you reset your password.
                    </p>
                </motion.div>

                {/* Message */}
                {message && (
                    <motion.div
                        initial={{ opacity: 0, y: -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className={`border px-4 py-3 rounded-xl mb-6 text-sm ${success
                            ? 'bg-green-500/10 border-green-500/50 text-green-300'
                            : 'bg-red-500/10 border-red-500/50 text-red-300'
                            }`}
                    >
                        {message}
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
                        {/* New Password */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                New Password *
                            </label>
                            <div className="relative">
                                <input
                                    name="newPassword"
                                    type={showNewPassword ? "text" : "password"}
                                    value={formData.newPassword}
                                    onChange={handleChange}
                                    required
                                    className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 pr-12 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                                    placeholder="Enter new password"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowNewPassword(!showNewPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition-colors"
                                >
                                    {showNewPassword ? (
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                                        </svg>
                                    ) : (
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                        </svg>
                                    )}
                                </button>
                            </div>
                            {passwordError && formData.newPassword && (
                                <p className="text-red-400 text-xs mt-1">{passwordError}</p>
                            )}
                            {!passwordError && formData.newPassword && (
                                <p className="text-green-400 text-xs mt-1">✓ Password meets requirements</p>
                            )}
                        </div>

                        {/* Confirm Password */}
                        <div>
                            <label className="block text-gray-400 text-sm font-medium mb-2">
                                Confirm Password *
                            </label>
                            <div className="relative">
                                <input
                                    name="confirmPassword"
                                    type={showConfirmPassword ? "text" : "password"}
                                    value={formData.confirmPassword}
                                    onChange={handleChange}
                                    required
                                    className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 pr-12 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                                    placeholder="Confirm new password"
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition-colors"
                                >
                                    {showConfirmPassword ? (
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                                        </svg>
                                    ) : (
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                        </svg>
                                    )}
                                </button>
                            </div>
                            {formData.confirmPassword && formData.newPassword !== formData.confirmPassword && (
                                <p className="text-red-400 text-xs mt-1">Passwords do not match</p>
                            )}
                            {formData.confirmPassword && formData.newPassword === formData.confirmPassword && (
                                <p className="text-green-400 text-xs mt-1">✓ Passwords match</p>
                            )}
                        </div>

                        {/* Submit Button */}
                        <div className="pt-2">
                            <motion.button
                                whileHover={{ scale: 1.02 }}
                                whileTap={{ scale: 0.98 }}
                                type="submit"
                                disabled={loading || passwordError || (formData.newPassword !== formData.confirmPassword)}
                                className="w-full bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-bold py-3 px-4 rounded-xl shadow-lg shadow-green-500/30 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {loading ? (
                                    <span className="flex items-center justify-center gap-2">
                                        <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                        </svg>
                                        Resetting...
                                    </span>
                                ) : (
                                    'Reset Password'
                                )}
                            </motion.button>
                        </div>
                    </motion.form>
                )}

                {success && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="text-center"
                    >
                        <p className="text-gray-400 text-sm">Redirecting to login...</p>
                    </motion.div>
                )}
            </motion.div>
        </div>
    );
};

export default ResetPassword;
