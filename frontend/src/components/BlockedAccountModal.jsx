import { motion } from 'framer-motion';

const BlockedAccountModal = () => {
    return (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
            <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                className="bg-gradient-to-br from-red-900/40 to-black/60 backdrop-blur-xl border-2 border-red-500/50 rounded-3xl p-8 sm:p-12 max-w-md w-full shadow-2xl"
            >
                {/* Warning Icon */}
                <div className="flex items-center justify-center w-20 h-20 bg-red-500/20 rounded-full mx-auto mb-6 animate-pulse">
                    <svg className="w-12 h-12 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                </div>

                {/* Title */}
                <h2 className="text-3xl sm:text-4xl font-bold text-red-400 text-center mb-4">
                    Account Blocked
                </h2>

                {/* Message */}
                <p className="text-gray-200 text-center mb-3 text-lg">
                    Your account has been disabled by an administrator.
                </p>

                <p className="text-gray-400 text-center mb-8">
                    If you believe this is a mistake, please contact our support team for assistance.
                </p>

                {/* Support Email */}
                <div className="bg-white/5 border border-white/10 rounded-xl p-4 mb-6">
                    <p className="text-sm text-gray-400 text-center mb-2">Contact Support:</p>
                    <a
                        href="mailto:support@codecombat.live"
                        className="text-green-400 hover:text-green-300 font-medium text-lg block text-center transition-colors"
                    >
                        support@codecombat.live
                    </a>
                </div>

                {/* Info */}
                <p className="text-xs text-gray-500 text-center">
                    You will be logged out automatically.
                </p>
            </motion.div>
        </div>
    );
};

export default BlockedAccountModal;
