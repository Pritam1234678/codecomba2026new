# Taste (Continuously Learned by [CommandCode][cmd])

[cmd]: https://commandcode.ai/

# communication
- Respond in Hinglish (Hindi + English mix) — match the user's communication style. Confidence: 0.90

# architecture
- Frontend is deployed on Vercel — do NOT build, modify, or deploy frontend to the VM; only backend runs on the Oracle VM. Confidence: 0.75

# ai-integration
- Use NVIDIA NIM API (integrate.api.nvidia.com/v1/chat/completions) with model moonshotai/kimi-k2.6 for AI-powered features. Confidence: 0.65

# ui-patterns
- Pre-fill profile/edit forms with data from AuthService/login context — don't create redundant fields (e.g., separate displayName) when the same data already exists in the user's auth profile. Confidence: 0.60
- Design achievement cards with pure CSS/HTML — modern aesthetic, no image files (WebP/PNG), asymmetric alignment. Confidence: 0.85

# git
- Git identity: Always use name=Pritam Mandal, email=mandalpritam765@gmail.com, GitHub=Pritam1234678. Never use --author, GIT_AUTHOR_*, GIT_COMMITTER_* env vars, bot identities, or local machine identity (e.g., pritam@kali.pritam). Never modify git config. All commits must attribute to Pritam1234678. No force-push unless explicitly asked. Confidence: 0.90

# workflow
- When removing a feature/field, always start from the backend (database schema, API, models) and then clean up the frontend — never remove frontend-only without backend changes. Confidence: 0.75
- Prefer thorough, exhaustive full-stack analysis over quick responses — analyze both frontend and backend together. Confidence: 0.70
- Verify before destructive DB operations — ask for explicit confirmation and do NOT proceed without it. Deleting the user entity itself is a separate action from cleaning related data. Confidence: 0.90
- When the user says "delete history" during proctoring testing, run full cleanup of proctoring data: SQL (screenshots, events, admin_audit, consent_acks, sessions, registrations), Valkey/Redis (proctoring:*), and disk screenshots — all scoped to the test user. Confidence: 0.70
- After code changes, deploy backend to VM: git push → ssh VM → git pull → ./mvnw -DskipTests clean package -q → cp target WAR to ~/app.war → sudo systemctl restart codecombat. Frontend is separate (Vercel). Confidence: 0.70

# coding-problems
See [coding-problems/taste.md](coding-problems/taste.md)
