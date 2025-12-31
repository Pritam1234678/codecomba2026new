import React from 'react';
import { Navigate } from 'react-router-dom';
import AuthService from '../services/auth.service';

// GuestRoute: Only accessible when NOT logged in
// If user is logged in, redirect to appropriate dashboard
const GuestRoute = ({ children }) => {
    const currentUser = AuthService.getCurrentUser();

    if (currentUser) {
        // User is logged in, redirect based on role
        if (currentUser.roles && currentUser.roles.includes('ROLE_ADMIN')) {
            return <Navigate to="/admin/dashboard" replace />;
        } else {
            return <Navigate to="/dashboard" replace />;
        }
    }

    // User is not logged in, allow access to login/register
    return children;
};

export default GuestRoute;
