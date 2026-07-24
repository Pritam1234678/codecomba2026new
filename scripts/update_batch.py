import re
with open('/home/ubuntu/batch_generate.py', 'r') as f:
    c = f.read()

# Update config
c = re.sub(r"SHEET_PATH = .*", "SHEET_PATH = '/home/ubuntu/Deloitte_100_Coding_Questions.xlsx'", c)
c = re.sub(r"MODEL\s*=.*", "MODEL   = \"moonshotai/kimi-k2.6\"", c)

# Add existing check: after reading questions, filter out existing
# Find the line "questions = [(name, diff) ..." section and add filter
old_questions_loop = """    questions = []
    for row in range(2, ws.max_row + 1):
        name = str(ws.cell(row, 4).value or "").strip()
        diff = str(ws.cell(row, 5).value or "").strip()
        if not name or name == "None":
            continue
        questions.append((name, diff))"""

new_questions_loop = """    questions = []
    for row in range(2, ws.max_row + 1):
        name = str(ws.cell(row, 4).value or "").strip()
        diff = str(ws.cell(row, 5).value or "").strip()
        if not name or name == "None":
            continue
        questions.append((name, diff))
    
    # Filter out existing problems
    import psycopg2
    try:
        conn = psycopg2.connect(host=DB_HOST, port=DB_PORT, dbname=DB_NAME, user=DB_USER, password=DB_PASS)
        cur = conn.cursor()
        cur.execute("SELECT LOWER(title) FROM problems")
        existing = {r[0].strip() for r in cur.fetchall()}
        cur.close(); conn.close()
        before = len(questions)
        questions = [(n, d) for n, d in questions if n.lower().strip() not in existing]
        print(f"Skipped {before - len(questions)} existing problems ({len(questions)} remaining)")
    except Exception as e:
        print(f"Could not check existing: {e}")"""

c = c.replace(old_questions_loop, new_questions_loop)

with open('/home/ubuntu/batch_generate.py', 'w') as f:
    f.write(c)
print('Updated batch_generate.py')
