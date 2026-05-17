import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import AuthService from '../services/auth.service';

const Footer = () => {
    const [currentUser, setCurrentUser] = useState(undefined);

    useEffect(() => {
        const user = AuthService.getCurrentUser();
        if (user) {
            setCurrentUser(user);
        }
    }, []);

    const isAdmin = currentUser && currentUser.roles && currentUser.roles.includes('ROLE_ADMIN');

    return (
        <footer className="bg-gradient-to-b from-black/50 to-black border-t border-[#1a1a1a] mt-auto w-full">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    {/* Brand Section */}
                    <div>
                        <h3 className="text-green-400 font-mono text-lg font-bold mb-3">
                            &lt;CodeCombat /&gt;
                        </h3>
                        <p className="text-gray-500 text-sm">
                            A competitive programming platform for students to practice and compete.
                        </p>
                    </div>

                    {/* Quick Links */}
                    <div>
                        <h4 className="text-gray-300 font-semibold mb-3 text-sm">Quick Links</h4>
                        <ul className="space-y-2">
                            <li>
                                <Link to={isAdmin ? "/admin/contests" : "/contests"} className="text-gray-500 hover:text-green-400 text-sm transition-colors">
                                    Contests
                                </Link>
                            </li>
                            <li>
                                <Link to={isAdmin ? "/admin/dashboard" : "/dashboard"} className="text-gray-500 hover:text-green-400 text-sm transition-colors">
                                    Dashboard
                                </Link>
                            </li>
                        </ul>
                    </div>

                    {/* Info */}
                    <div>
                        <h4 className="text-gray-300 font-semibold mb-3 text-sm">Platform Info</h4>
                        <ul className="space-y-2 text-sm text-gray-500">
                            <li>Built with React & Spring Boot</li>
                            <li>Powered by Docker Judge System</li>
                            <li> Made by Pritam</li>

                        </ul>
                    </div>
                </div>

                {/* Bottom Bar */}
                <div className="mt-8 pt-6 border-t border-[#1a1a1a]">
                    <div className="flex flex-col md:flex-row justify-between items-center">
                        <p className="text-gray-600 text-sm">
                            © {new Date().getFullYear()} CodeCombat. All rights reserved.
                        </p>
                        <p className="text-gray-600 text-sm mt-2 md:mt-0">
                            Made with <span className="text-green-400">❤</span> for competitive programmers
                        </p>
                    </div>
                </div>
            </div>
        </footer>
    );
};

export default Footer;
