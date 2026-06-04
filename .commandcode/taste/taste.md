# Taste (Continuously Learned by [CommandCode][cmd])

[cmd]: https://commandcode.ai/

# communication
- Respond in Hinglish (Hindi + English mix) — match the user's communication style. Confidence: 0.75

# architecture
- Frontend is deployed on Vercel — do NOT build, modify, or deploy frontend to the VM; only backend runs on the Oracle VM. Confidence: 0.75

# workflow
- Prefer thorough, exhaustive full-stack analysis over quick responses — analyze both frontend and backend together. Confidence: 0.70
- Verify before destructive DB operations — ask for explicit confirmation and do NOT proceed without it. Deleting the user entity itself is a separate action from cleaning related data. Confidence: 0.90
- When the user says "delete history" during proctoring testing, run full cleanup of proctoring data: SQL (screenshots, events, admin_audit, consent_acks, sessions, registrations), Valkey/Redis (proctoring:*), and disk screenshots — all scoped to the test user. Confidence: 0.70
- After code changes, deploy backend to VM: git push → ssh VM → git pull → ./mvnw -DskipTests clean package -q → cp target WAR to ~/app.war → sudo systemctl restart codecombat. Frontend is separate (Vercel). Confidence: 0.70

