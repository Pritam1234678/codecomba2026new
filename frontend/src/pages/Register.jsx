import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import AuthService from '../services/auth.service';

const Register = () => {
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
    fullName: "",
    rollNumber: "",
    branch: "",
    phoneNumber: ""
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleRegister = (e) => {
    e.preventDefault();
    setMessage("");
    setLoading(true);

    AuthService.register(
      formData.username,
      formData.email,
      formData.password,
      formData.fullName,
      formData.rollNumber,
      formData.branch,
      formData.phoneNumber
    ).then(
      () => {
        setLoading(false);
        setMessage("Registration successful! Redirecting to login...");
        setTimeout(() => navigate("/login"), 2000);
      },
      (error) => {
        const resMessage =
          (error.response &&
            error.response.data &&
            error.response.data.message) ||
          error.message ||
          error.toString();

        setLoading(false);
        setMessage(resMessage);
      }
    );
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="bg-gradient-to-br from-white/5 to-white/[0.02] backdrop-blur-xl border border-white/10 p-8 md:p-12 rounded-2xl shadow-2xl w-full max-w-3xl"
      >
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="text-center mb-8"
        >
          <h2 className="text-4xl md:text-5xl font-bold mb-2">
            <span className="text-white">Join the </span>
            <span className="text-green-400">Arena</span>
          </h2>
          <p className="text-gray-400 text-sm">Create your account and start competing</p>
        </motion.div>

        {/* Message */}
        {message && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className={`border px-4 py-3 rounded-xl mb-6 text-sm backdrop-blur-sm ${message.includes("success")
              ? "bg-green-500/10 border-green-500/50 text-green-300"
              : "bg-red-500/10 border-red-500/50 text-red-300"
              }`}
          >
            {message}
          </motion.div>
        )}

        {/* Registration Form */}
        <motion.form
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          onSubmit={handleRegister}
          className="space-y-5"
        >
          {/* Username & Email Row */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Username */}
            <div>
              <label className="block text-gray-400 text-sm font-medium mb-2">
                Username *
              </label>
              <input
                name="username"
                type="text"
                onChange={handleChange}
                required
                className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                placeholder="Choose a username"
              />
            </div>

            {/* Email */}
            <div>
              <label className="block text-gray-400 text-sm font-medium mb-2">
                Email *
              </label>
              <input
                name="email"
                type="email"
                onChange={handleChange}
                required
                className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                placeholder="your.email@example.com"
              />
            </div>
          </div>

          {/* Password */}
          <div>
            <label className="block text-gray-400 text-sm font-medium mb-2">
              Password *
            </label>
            <input
              name="password"
              type="password"
              onChange={handleChange}
              required
              className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
              placeholder="Create a strong password"
            />
          </div>

          {/* Full Name */}
          <div>
            <label className="block text-gray-400 text-sm font-medium mb-2">
              Full Name
            </label>
            <input
              name="fullName"
              type="text"
              onChange={handleChange}
              className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
              placeholder="Your full name"
            />
          </div>

          {/* Phone Number */}
          <div>
            <label className="block text-gray-400 text-sm font-medium mb-2">
              Phone Number
            </label>
            <input
              name="phoneNumber"
              type="text"
              onChange={handleChange}
              className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
              placeholder="+91 1234567890"
            />
          </div>

          {/* Roll Number & Branch Row */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Roll Number */}
            <div>
              <label className="block text-gray-400 text-sm font-medium mb-2">
                Roll Number
              </label>
              <input
                name="rollNumber"
                type="text"
                onChange={handleChange}
                className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                placeholder="Your roll number"
              />
            </div>

            {/* Branch */}
            <div>
              <label className="block text-gray-400 text-sm font-medium mb-2">
                Branch
              </label>
              <input
                name="branch"
                type="text"
                onChange={handleChange}
                className="w-full bg-white/5 text-white border border-white/10 rounded-xl py-3 px-4 focus:outline-none focus:border-green-500/50 focus:ring-2 focus:ring-green-500/20 transition-all placeholder-gray-500"
                placeholder="e.g., Computer Science"
              />
            </div>
          </div>

          {/* Register Button */}
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
                  Creating account...
                </span>
              ) : (
                "Create Account"
              )}
            </motion.button>
          </div>
        </motion.form>

        {/* Login Link */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.5 }}
          className="mt-8 text-center"
        >
          <p className="text-gray-400 text-sm">
            Already have an account?{" "}
            <Link
              to="/login"
              className="text-green-400 hover:text-green-300 font-semibold transition-colors"
            >
              Login here
            </Link>
          </p>
        </motion.div>
      </motion.div>
    </div>
  );
};

export default Register;
