/**
 * Hosting Request Page
 * 
 * Allows users to request hosting privileges to create private contests.
 * Shows request form or current status if already submitted.
 */

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import PrivateContestService from '../services/privateContest.service';

const HostingRequest = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [status, setStatus] = useState(null);
    const [error, setError] = useState('');

    const [formData, setFormData] = useState({
        reason: '',
        intendedUseCase: 'EDUCATION'
    });

    useEffect(() => {
        checkStatus();
    }, []);

    const checkStatus = async () => {
        try {
            const response = await PrivateContestService.getMyHostingStatus();
            setStatus(response.data);
            setLoading(false);
        } catch (err) {
            console.error('Failed to check hosting status:', err);
            setLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSubmitting(true);

        try {
            await PrivateContestService.submitHostingRequest(
                formData.reason,
                formData.intendedUseCase
            );
            // Refresh status
            await checkStatus();
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to submit hosting request');
        } finally {
            setSubmitting(false);
        }
    };

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="text-gray-400">Loading...</div>
            </div>
        );
    }

    // If already approved, redirect to create contest
    if (status?.canCreateContests) {
        return (
            <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
                <div className="max-w-3xl mx-auto">
                    <div className="bg-[#1c1b1b] border border-green-500/30 rounded-xl p-8">
                        <div className="flex items-center gap-4 mb-6">
                            <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                            </div>
                            <div>
                                <h2 className="text-2xl font-bold text-green-500">Hosting Approved</h2>
                                <p className="text-gray-400 mt-1">You can now create private contests</p>
                            </div>
                        </div>

                        <div className="flex gap-4">
                            <button
                                onClick={() => navigate('/contests/private/create')}
                                className="px-6 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition-colors"
                            >
                                Create Private Contest
                            </button>
                            <button
                                onClick={() => navigate('/contests/private/my-contests')}
                                className="px-6 py-3 bg-gray-700 hover:bg-gray-600 rounded-lg font-semibold transition-colors"
                            >
                                View My Contests
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    // If request is pending
    if (status?.hasRequest && status?.status === 'PENDING') {
        return (
            <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
                <div className="max-w-3xl mx-auto">
                    <div className="bg-[#1c1b1b] border border-yellow-500/30 rounded-xl p-8">
                        <div className="flex items-center gap-4 mb-6">
                            <div className="w-16 h-16 bg-yellow-500/20 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-yellow-500 animate-spin" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                            </div>
                            <div>
                                <h2 className="text-2xl font-bold text-yellow-500">Request Pending</h2>
                                <p className="text-gray-400 mt-1">Your hosting request is under review</p>
                            </div>
                        </div>

                        <div className="bg-[#0f0f0f] rounded-lg p-6 mb-6">
                            <div className="space-y-3">
                                <div>
                                    <span className="text-gray-500 text-sm">Submitted:</span>
                                    <p className="text-gray-200">{new Date(status.submittedAt).toLocaleString()}</p>
                                </div>
                                <div>
                                    <span className="text-gray-500 text-sm">Reason:</span>
                                    <p className="text-gray-200">{status.request.reason}</p>
                                </div>
                                <div>
                                    <span className="text-gray-500 text-sm">Use Case:</span>
                                    <p className="text-gray-200">{status.request.intendedUseCase}</p>
                                </div>
                            </div>
                        </div>

                        <p className="text-gray-400 text-sm">
                            An administrator will review your request shortly. You'll be notified via email once approved.
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    // If request was rejected
    if (status?.hasRequest && status?.status === 'REJECTED') {
        return (
            <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
                <div className="max-w-3xl mx-auto">
                    <div className="bg-[#1c1b1b] border border-red-500/30 rounded-xl p-8">
                        <div className="flex items-center gap-4 mb-6">
                            <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </div>
                            <div>
                                <h2 className="text-2xl font-bold text-red-500">Request Rejected</h2>
                                <p className="text-gray-400 mt-1">Your hosting request was not approved</p>
                            </div>
                        </div>

                        {status.request.adminNotes && (
                            <div className="bg-[#0f0f0f] rounded-lg p-6 mb-6">
                                <span className="text-gray-500 text-sm">Admin Notes:</span>
                                <p className="text-gray-200 mt-2">{status.request.adminNotes}</p>
                            </div>
                        )}

                        <p className="text-gray-400 text-sm">
                            If you believe this was a mistake, please contact support at support@codecombat.live
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    // Show request form
    return (
        <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
            <div className="max-w-3xl mx-auto">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold mb-2">Request Hosting Privileges</h1>
                    <p className="text-gray-400">
                        To create private contests, you need hosting privileges. Fill out this form to request access.
                    </p>
                </div>

                <div className="bg-[#1c1b1b] rounded-xl p-8 border border-gray-800">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {error && (
                            <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-4 text-red-400">
                                {error}
                            </div>
                        )}

                        <div>
                            <label className="block text-sm font-semibold text-gray-300 mb-2">
                                Intended Use Case *
                            </label>
                            <select
                                name="intendedUseCase"
                                value={formData.intendedUseCase}
                                onChange={handleChange}
                                required
                                className="w-full bg-[#0f0f0f] border border-gray-700 rounded-lg px-4 py-3 text-gray-100 focus:outline-none focus:border-blue-500"
                            >
                                <option value="EDUCATION">Education (University/School)</option>
                                <option value="RECRUITMENT">Recruitment/Hiring</option>
                                <option value="COMPETITION">Coding Competition</option>
                                <option value="TRAINING">Corporate Training</option>
                                <option value="OTHER">Other</option>
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-300 mb-2">
                                Reason for Request *
                            </label>
                            <textarea
                                name="reason"
                                value={formData.reason}
                                onChange={handleChange}
                                required
                                maxLength={500}
                                rows={6}
                                placeholder="Please explain why you need hosting privileges. Include details about your organization, how you plan to use private contests, and the expected number of participants."
                                className="w-full bg-[#0f0f0f] border border-gray-700 rounded-lg px-4 py-3 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-blue-500 resize-none"
                            />
                            <div className="text-right text-sm text-gray-500 mt-1">
                                {formData.reason.length}/500
                            </div>
                        </div>

                        <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4">
                            <h3 className="font-semibold text-blue-400 mb-2">What You'll Get</h3>
                            <ul className="space-y-2 text-sm text-gray-300">
                                <li className="flex items-start gap-2">
                                    <svg className="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                    Create unlimited private contests
                                </li>
                                <li className="flex items-start gap-2">
                                    <svg className="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                    Invite up to 100 participants per contest
                                </li>
                                <li className="flex items-start gap-2">
                                    <svg className="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                    AI-powered problem generation
                                </li>
                                <li className="flex items-start gap-2">
                                    <svg className="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                    Real-time leaderboards and analytics
                                </li>
                                <li className="flex items-start gap-2">
                                    <svg className="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                    </svg>
                                    Optional proctoring for integrity
                                </li>
                            </ul>
                        </div>

                        <div className="flex gap-4">
                            <button
                                type="submit"
                                disabled={submitting}
                                className="flex-1 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-700 disabled:cursor-not-allowed rounded-lg font-semibold transition-colors"
                            >
                                {submitting ? 'Submitting...' : 'Submit Request'}
                            </button>
                            <button
                                type="button"
                                onClick={() => navigate('/dashboard')}
                                className="px-6 py-3 bg-gray-700 hover:bg-gray-600 rounded-lg font-semibold transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default HostingRequest;
