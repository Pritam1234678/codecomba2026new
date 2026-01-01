import React, { useState } from 'react';
import { motion } from 'framer-motion';

const Support = () => {
    const [formData, setFormData] = useState({
        fullName: '',
        email: '',
        phone: '',
        message: ''
    });
    const [submitted, setSubmitted] = useState(false);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage('');

        try {
            const response = await fetch('/api/support/send', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                setSubmitted(true);
                setTimeout(() => {
                    setSubmitted(false);
                    setFormData({ fullName: '', email: '', phone: '', message: '' });
                }, 3000);
            } else {
                setMessage('Failed to send message. Please try again.');
            }
        } catch (error) {
            setMessage('Error sending message. Please try again later.');
        } finally {
            setLoading(false);
        }
    };

    const faqs = [
        {
            question: 'What programming languages are supported?',
            answer: 'We support all major programming languages including C, C++, Java, Python, and JavaScript. Choose the language you\'re most comfortable with.'
        },
        {
            question: 'How do I participate in a contest?',
            answer: 'Navigate to the Contests page, select an active contest, and click "Participate". You can then solve problems and submit your solutions during the contest duration.'
        },
        {
            question: 'What should I do if my code doesn\'t compile?',
            answer: 'Make sure your code follows the correct syntax for the selected language. Check for common errors like missing semicolons, brackets, or incorrect function signatures. You can also test your code locally before submitting.'
        },
        {
            question: 'Is there a registration fee?',
            answer: 'No, CodeCombat is completely free to participate! This is our way of giving back to the programming community.'
        },
        {
            question: 'How is the leaderboard calculated?',
            answer: 'The leaderboard is based on the number of problems solved correctly and the time taken to solve them. Faster and more accurate solutions rank higher.'
        },
        {
            question: 'Can I edit my submission after submitting?',
            answer: 'Once submitted, you cannot edit a submission. However, you can submit multiple times for the same problem, and your best submission will be considered.'
        }
    ];

    return (
        <div className="min-h-screen bg-gradient-to-b from-black via-green-950/20 to-black py-8 px-4 sm:px-6 lg:px-8">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5 }}
                    className="text-center mb-12"
                >
                    <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold text-green-400 mb-4">
                        Need Support?
                    </h1>
                    <p className="text-gray-400 text-sm sm:text-base max-w-2xl mx-auto">
                        We're here to help! Whether you have technical issues, registration questions, or just want to know more about the competition, reach out to us.
                    </p>
                </motion.div>

                {/* Contact Form */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, delay: 0.2 }}
                    className="bg-gradient-to-br from-white/5 to-white/[0.02] backdrop-blur-xl border border-white/10 rounded-2xl p-6 sm:p-8 lg:p-10 mb-12"
                >
                    <h2 className="text-2xl sm:text-3xl font-semibold text-green-400 mb-6 text-center">
                        Send us a Message
                    </h2>

                    {submitted && (
                        <motion.div
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            className="bg-green-500/10 border border-green-500/50 text-green-300 px-4 py-3 rounded-xl mb-6 text-center"
                        >
                            ✓ Message sent successfully! We'll get back to you soon.
                        </motion.div>
                    )}

                    {message && (
                        <motion.div
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            className="bg-red-500/10 border border-red-500/50 text-red-300 px-4 py-3 rounded-xl mb-6 text-center"
                        >
                            {message}
                        </motion.div>
                    )}

                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            {/* Full Name */}
                            <div>
                                <label className="block text-gray-300 text-sm font-medium mb-2">
                                    Full Name <span className="text-green-400">*</span>
                                </label>
                                <input
                                    type="text"
                                    name="fullName"
                                    value={formData.fullName}
                                    onChange={handleChange}
                                    required
                                    className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                                    placeholder="Enter your full name"
                                />
                            </div>

                            {/* Email */}
                            <div>
                                <label className="block text-gray-300 text-sm font-medium mb-2">
                                    Email Address <span className="text-green-400">*</span>
                                </label>
                                <input
                                    type="email"
                                    name="email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    required
                                    className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                                    placeholder="Enter your email address"
                                />
                            </div>
                        </div>

                        {/* Phone */}
                        <div>
                            <label className="block text-gray-300 text-sm font-medium mb-2">
                                Phone Number
                            </label>
                            <input
                                type="tel"
                                name="phone"
                                value={formData.phone}
                                onChange={handleChange}
                                className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                                placeholder="Enter your phone number (optional)"
                            />
                        </div>

                        {/* Message */}
                        <div>
                            <label className="block text-gray-300 text-sm font-medium mb-2">
                                Message <span className="text-green-400">*</span>
                            </label>
                            <textarea
                                name="message"
                                value={formData.message}
                                onChange={handleChange}
                                required
                                rows="5"
                                className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500 resize-none"
                                placeholder="Type your message here..."
                            ></textarea>
                        </div>

                        {/* Submit Button */}
                        <motion.button
                            whileHover={{ scale: 1.02 }}
                            whileTap={{ scale: 0.98 }}
                            type="submit"
                            className="w-full bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-bold py-3 px-6 rounded-xl shadow-lg shadow-green-500/30 transition-all"
                        >
                            Send Message
                        </motion.button>
                    </form>
                </motion.div>

                {/* Help Sections */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-12">
                    {/* Registration Help */}
                    <motion.div
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.5, delay: 0.3 }}
                        className="bg-gradient-to-br from-white/5 to-white/[0.02] backdrop-blur-xl border border-white/10 rounded-2xl p-6 sm:p-8"
                    >
                        <div className="flex items-center justify-center w-12 h-12 bg-green-500/10 rounded-xl mb-4 mx-auto">
                            <svg className="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                            </svg>
                        </div>
                        <h3 className="text-xl font-semibold text-green-400 mb-3 text-center">
                            Registration Help
                        </h3>
                        <p className="text-gray-400 text-sm text-center mb-4">
                            Having trouble with registration, team formation, or need to update your information?
                        </p>
                        <a
                            href="mailto:support@codecombat.live"
                            className="block text-center text-green-400 hover:text-green-300 font-medium transition-colors"
                        >
                            support@codecombat.live
                        </a>
                    </motion.div>

                    {/* General Inquiries */}
                    <motion.div
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.5, delay: 0.4 }}
                        className="bg-gradient-to-br from-white/5 to-white/[0.02] backdrop-blur-xl border border-white/10 rounded-2xl p-6 sm:p-8"
                    >
                        <div className="flex items-center justify-center w-12 h-12 bg-green-500/10 rounded-xl mb-4 mx-auto">
                            <svg className="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                        </div>
                        <h3 className="text-xl font-semibold text-green-400 mb-3 text-center">
                            General Inquiries
                        </h3>
                        <p className="text-gray-400 text-sm text-center mb-4">
                            Questions about rules, prizes, event schedule, or anything else about CodeCombat?
                        </p>
                        <a
                            href="mailto:support@codecombat.live"
                            className="block text-center text-green-400 hover:text-green-300 font-medium transition-colors"
                        >
                            support@codecombat.live
                        </a>
                    </motion.div>
                </div>

                {/* FAQs */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, delay: 0.5 }}
                    className="bg-gradient-to-br from-white/5 to-white/[0.02] backdrop-blur-xl border border-white/10 rounded-2xl p-6 sm:p-8 lg:p-10"
                >
                    <h2 className="text-2xl sm:text-3xl font-semibold text-green-400 mb-8 text-center">
                        Frequently Asked Questions
                    </h2>
                    <div className="space-y-4">
                        {faqs.map((faq, index) => (
                            <motion.div
                                key={index}
                                initial={{ opacity: 0, y: 10 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ duration: 0.3, delay: 0.6 + index * 0.1 }}
                                className="bg-white/5 border border-white/10 rounded-xl p-4 sm:p-6 hover:border-green-500/30 transition-all"
                            >
                                <h3 className="text-base sm:text-lg font-semibold text-green-400 mb-2">
                                    {faq.question}
                                </h3>
                                <p className="text-gray-400 text-sm">
                                    {faq.answer}
                                </p>
                            </motion.div>
                        ))}
                    </div>
                </motion.div>
            </div>
        </div>
    );
};

export default Support;
