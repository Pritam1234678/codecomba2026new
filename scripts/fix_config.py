import re
with open('/home/ubuntu/batch_generate.py', 'r') as f:
    c = f.read()
c = re.sub(r"SHEET_PATH = .*", "SHEET_PATH = '/home/ubuntu/Deloitte_100_Coding_Questions.xlsx'", c)
c = re.sub(r"DB_HOST = os.environ.get\('DB_HOST', 'localhost'\)", "DB_HOST = os.environ.get('DB_HOST', 'localhost')", c)
c = re.sub(r"DB_PORT = os.environ.get\('DB_PORT', '5432'\)", "DB_PORT = os.environ.get('DB_PORT', '5432')", c)
c = re.sub(r"DB_NAME = os.environ.get\('DB_NAME', 'codecombat'\)", "DB_NAME = os.environ.get('DB_NAME', 'codecombat')", c)
c = re.sub(r"DB_USER = os.environ.get\('DB_USER', 'postgres'\)", "DB_USER = os.environ.get('DB_USER', 'postgres')", c)
c = re.sub(r"DB_PASS = os.environ.get\('DB_PASSWORD', 'postgres'\)", "DB_PASS = os.environ.get('DB_PASSWORD', 'postgres')", c)
with open('/home/ubuntu/batch_generate.py', 'w') as f:
    f.write(c)
print('Done')
