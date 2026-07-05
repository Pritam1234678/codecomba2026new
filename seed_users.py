#!/usr/bin/env python3
"""Seed 1000 dummy users into the production PostgreSQL database.

Schema:
  users(id, email, enabled, full_name, password, username, total_points)
  user_roles(user_id, role_id)  -- role_id=1 is ROLE_USER
"""

import bcrypt
import psycopg2

DB = dict(host="localhost", port=5432, dbname="codecombat",
          user="postgres", password="postgres")

RAW_PASSWORD = b"Dummy@12345"
HASHED = bcrypt.hashpw(RAW_PASSWORD, bcrypt.gensalt(rounds=10)).decode()
ROLE_USER_ID = 1   # SELECT id FROM roles WHERE name='ROLE_USER'

def seed():
    conn = psycopg2.connect(**DB)
    cur  = conn.cursor()

    inserted = 0
    skipped  = 0
    for i in range(1, 1001):
        username  = f"dummy{i}"
        email     = f"dummy{i}@codecoder.in"
        full_name = f"Dummy User {i}"

        cur.execute("SELECT id FROM users WHERE email = %s OR username = %s", (email, username))
        if cur.fetchone():
            skipped += 1
            continue

        cur.execute(
            """INSERT INTO users (email, enabled, full_name, password, username, total_points)
               VALUES (%s, true, %s, %s, %s, 0) RETURNING id""",
            (email, full_name, HASHED, username)
        )
        uid = cur.fetchone()[0]
        cur.execute("INSERT INTO user_roles (user_id, role_id) VALUES (%s, %s)", (uid, ROLE_USER_ID))
        inserted += 1
        if inserted % 100 == 0:
            conn.commit()
            print(f"  committed {inserted} so far …")

    conn.commit()
    cur.close()
    conn.close()
    print(f"\nDone. Inserted: {inserted}  Skipped (already existed): {skipped}")

if __name__ == "__main__":
    print("Seeding 1000 dummy users …")
    seed()
