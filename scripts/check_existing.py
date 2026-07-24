import psycopg2
conn = psycopg2.connect(host='localhost', port=5432, dbname='codecombat', user='postgres', password='postgres')
cur = conn.cursor()
cur.execute("SELECT LOWER(title) FROM problems")
existing = {r[0].strip() for r in cur.fetchall()}
print(f"Existing problems: {len(existing)}")
for t in sorted(existing):
    print(f"  {t}")
cur.close(); conn.close()
