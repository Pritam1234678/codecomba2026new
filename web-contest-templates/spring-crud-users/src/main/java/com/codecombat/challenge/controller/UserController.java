package com.codecombat.challenge.controller;

// ═══════════════════════════════════════════════════════════════
// YOUR TASK: Build a REST Controller for User CRUD operations.
//
// Available:
//   - UserService (injected) with methods:
//       getAllUsers() → List<User>
//       getUserById(Long id) → Optional<User>
//       createUser(User user) → User
//       deleteUser(Long id) → void
//       existsById(Long id) → boolean
//
//   - User entity has: id (Long), name (String), email (String)
//     name is @NotBlank — validation must be enforced
//
// Required endpoints:
//   GET    /api/users       → return all users (200)
//   POST   /api/users       → create user (201 Created)
//   GET    /api/users/{id}  → get by id (200) or 404 if not found
//   DELETE /api/users/{id}  → delete (204 No Content) or 404
//
// Hints:
//   - Use @Valid on @RequestBody for validation
//   - Return proper HTTP status codes
//   - Use ResponseEntity for custom status codes
// ═══════════════════════════════════════════════════════════════

// Write your code below:

