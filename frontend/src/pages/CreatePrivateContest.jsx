/**
 * Create Private Contest Page
 * 
 * Allows approved hosts to create new private contests.
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PrivateContestService from '../services/privateContest.service';

const CreatePrivateContest = () => {
    const navigate = useNavigate();
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [created, setCreated] = useState(null);

    const [formData, setFormData] = useState({
        name: '',
        description: '',
        startTime: '',
        endTime: '',
        enableProctoring: false
    });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSubmitting(true);

        try {
            const response = await PrivateContestService.createPrivateContest(formData);
            setCreated(response.data);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to create contest');
            setSubmitting(false);
        }
    };

    const handleChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData({
            ...formData,
            [name]: type === 'checkbox' ? checked : value
        });
    };

    const copyInviteLink = () => {
        navigator.clipboard.writeText(created.inviteLink);
        alert('Invite link copied to clipboard!');
    };

    // Success screen with invite link
    if (created) {
        return (
            <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
                <div className="max-w-4xl mx-auto">
                    <div className="bg-[#1c1b1b] rounded-xl p-8 border border-green-500/30">
                        <div className="flex items-center gap-4 mb-6">
                            <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                            </div>
                            <div>
                                <h2 className="text-2xl font-bold text-green-500">Contest Created Successfully!</h2>
                                <p className="text-gray-400 mt-1">Your private contest is ready</p>
                            </div>
                        </div>

                        <div className="space-y-6">
                            <div className="bg-[#0f0f0f] rounded-lg p-6">
                                <h3 className="font-semibold text-gray-300 mb-4">Contest Details</h3>
                                <div className="space-y-3 text-sm">
                                    <div>
                                        <span className="text-gray-500">Name:</span>
                                        <p className="text-gray-200 font-semibold">{created.name}</p>
                                    </div>
                                    <div>
                                        <span className="text-gray-500">Contest ID:</span>
                                        <p className="text-gray-200 font-mono">{created.contestId}</p>
                                    </div>
                                    <div>
                                        <span className="text-gray-500">Start Time:</span>
                                        <p className="text-gray-200">{new Date(created.startTime).toLocaleString()}</p>
                                    </div>
                                    <div>
                                        <span className="text-gray-500">End Time:</span>
                                        <p className="text-gray-200">{new Date(created.endTime).toLocaleString()}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-6">
                                <h3 className="font-semibold text-blue-400 mb-3">Share Invite Link</h3>
                                <p className="text-gray-400 text-sm mb-4">
                                    Share this link with participants to let them join your contest
                                </p>
                                <div className="flex gap-2">
                                    <input
                                        type="text"
                                        value={created.inviteLink}
                                        readOnly
                                        className="flex-1 bg-[#0f0f0f] border border-gray-700 rounded-lg px-4 py-2 text-gray-300 font-mono text-sm"
                                    />
                                    <button
                                        onClick={copyInviteLink}
                                        className="px-6 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition-colors flex items-center gap-2"
                                    >
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
                                        </svg>
                                        Copy
                                    </button>
                                </div>
                            </div>

                            <div className="flex gap-4">
                                <button
                                    onClick={() => navigate(`/contests/private/${created.id}/manage`)}
                                    className="flex-1 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition-colors"
                                >
                                    Manage Contest
                                </button>
                                <button
                                    onClick={() => navigate('/contests/private/my-contests')}
                                    className="px-6 py-3 bg-gray-700 hover:bg-gray-600 rounded-lg font-semibold transition-colors"
                                >
                                    View All Contests
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    // Creation form
    return (
        <div className="min-h-screen bg-[#131313] text-gray-100 p-8">
            <div className="max-w-4xl mx-auto">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold mb-2">Create Private Contest</h1>
                    <p className="text-gray-400">
                        Set up a new private contest for your participants
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
                                Contest Name *
                            </label>
                            <input
                                type="text"
                                name="name"
                                value={formData.name}
                                onChange={handleChange}
                                required
                                maxLength={100}
                                placeholder="e.g., CS101 Midterm Exam"
                                className="w-full bg-[#0f0f0f] border border-gray-700 rounded-lg px-4 py-3 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-blue-500"
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-300 mb-2">
                                Description
                            </label>
                            <textarea
                                name="description"
                                value={formData.description}
                                onChange={handleChange}
                                rows={4}
                                maxLength={500}
                                placeholder="Describe your contest, its purpose, and any special instructions for participants"
                                className="w-full bg-[#0f0f0f] border border-gray-700 rounded-lg px-4 py-3 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-blue-500 resize-none"
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-6">
                            <div>
                                <label className="block text-sm font-semibold text-gray-300 mb-2">
                                    Start Time *
                                </label>
                                <input
                                    type="datetime-local"
                                    name="startTime"
                                    value={formData.startTime}
                                    onChange={handleChange}
                                    required
                                    className="w-full bg-[#0f0f0f] border border-gray-700 rounded-lg px-4 py-3 text-gray-100 focus:outline-none focus:border-blue-500"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-semibold text-gray-300 mb-2">
                                    End Time *
                                </label>
                                <input
                                    type="datetime-local"
                                    name="endTime"
                                    value={formData.endTime}
                                    onChange={handleChange}
                                    required
                                    className="w-full bg-[#0f0f0f] border border-gray-700 rounded-lg px-4 py-3 text-gray-100 focus:outline-none focus:border-blue-500"
                                />
                            </div>
                        </div>

                        <div className="bg-[#0f0f0f] border border-gray-700 rounded-lg p-6">
                            <label className="flex items-start gap-4 cursor-pointer">
                                <input
                                    type="checkbox"
                                    name="enableProctoring"
                                    checked={formData.enableProctoring}
                                    onChange={handleChange}
                                    className="w-5 h-5 mt-1 bg-[#1c1b1b] border-gray-600 rounded focus:ring-blue-500 focus:ring-offset-0"
                                />
                                <div className="flex-1">
                                    <div className="font-semibold text-gray-200 mb-1">Enable Proctoring</div>
                                    <p className="text-sm text-gray-400">
                                        Monitor participants for suspicious activity using webcam and browser detection.
                                        Participants will need to grant camera permission and accept proctoring terms.
                                    </p>
                                </div>
                            </label>
                        </div>

                        <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-4">
                            <div className="flex items-start gap-3">
                                <svg className="w-5 h-5 text-yellow-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                                <div className="text-sm text-gray-300">
                                    <p className="font-semibold text-yellow-500 mb-1">Next Steps</p>
                                    <ul className="space-y-1 list-disc list-inside">
                                        <li>After creation, you'll receive an invite link to share</li>
                                        <li>Add problems to your contest from our library or generate with AI</li>
                                        <li>Participants join using the invite link</li>
                                        <li>Contest starts automatically at the scheduled time</li>
                                    </ul>
                                </div>
                            </div>
                        </div>

                        <div className="flex gap-4">
                            <button
                                type="submit"
                                disabled={submitting}
                                className="flex-1 py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-700 disabled:cursor-not-allowed rounded-lg font-semibold transition-colors"
                            >
                                {submitting ? 'Creating Contest...' : 'Create Contest'}
                            </button>
                            <button
                                type="button"
                                onClick={() => navigate('/contests/private/my-contests')}
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

export default CreatePrivateContest;
