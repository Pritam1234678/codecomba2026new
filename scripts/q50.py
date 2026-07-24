"""
Simplify Path
==============
Given a string path which is an absolute Unix-style path (starting with '/'),
convert it to simplified canonical form.

Rules:
1. Single dot '.' means current directory — ignore it.
2. Double dot '..' means parent directory — pop one level.
3. Multiple consecutive '/' should be treated as single '/'.
4. The path must start with a single '/'.
5. The path must NOT end with '/' unless it's the root.
6. The simplified path must be the shortest possible.

Examples:
  "/home/"          → "/home"
  "/../"            → "/"       (cannot go above root)
  "/home//foo/"     → "/home/foo"
  "/a/./b/../../c/" → "/c"

Approach: Split by '/', use a stack to handle directory names.
- If component is empty or '.', skip.
- If component is '..', pop from stack if not empty.
- Otherwise, push component onto stack.

10 test cases — 5 visible, 5 hidden. Class name: CodeCoder
"""
import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Simplify Path"
desc = (
    "Given a string path which is an absolute Unix-style path (starting with '/'), "
    "return the simplified canonical form.\n\n"
    "The canonical path must have the following rules:\n"
    "1. Starts with a single slash '/'.\n"
    "2. Directories within are separated by a single slash '/'.\n"
    "3. Must NOT end with '/' unless it's the root path.\n"
    "4. Single dot '.' refers to current directory — ignore it.\n"
    "5. Double dot '..' refers to the parent directory — go up one level.\n"
    "6. Any sequence of consecutive slashes is treated as one slash.\n\n"
    "Think of it like navigating a file system: start at root '/', "
    "go into directories by name, go back with '..', stay with '.'.\n\n"
    "For example:\n"
    "- '/home/' → '/home' (remove trailing slash)\n"
    "- '/../' → '/' (cannot go above root)\n"
    "- '/home//foo/' → '/home/foo' (merge double slashes)"
)
infmt = "Single line containing the absolute path string."
outfmt = "Print the simplified canonical path."
cons = (
    "1 \u2264 |path| \u2264 3000\n"
    "path consists of English letters, digits, '.', '/' or '_'.\n"
    "path is a valid absolute Unix path."
)
e1 = (
    "Input:\n"
    "/home/\n\n"
    "Output:\n"
    "/home\n\n"
    "Explanation: Trailing slash is removed."
)
e2 = (
    "Input:\n"
    "/../\n\n"
    "Output:\n"
    "/\n\n"
    "Explanation: Cannot go above root, so stay at root."
)
e3 = (
    "Input:\n"
    "/home//foo/\n\n"
    "Output:\n"
    "/home/foo\n\n"
    "Explanation: Multiple slashes are replaced by single slash."
)

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True,
     "String, Stack", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

java_code = r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String simplifyPath(String path) {
        // Write your code here — use a stack
        return "";
    }
}
// USER_CODE_END

public class Main {
    static void test(String path, String expected, int tc, boolean hidden) {
        String got = new CodeCoder().simplifyPath(path);
        if (got.equals(expected)) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:path=" + path
                + ":expected=" + expected + ":got=" + got);
        }
    }

    public static void main(String[] args) {
        try { test("/home/", "/home", 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }

        try { test("/../", "/", 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }

        try { test("/home//foo/", "/home/foo", 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }

        try { test("/a/./b/../../c/", "/c", 4, false); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }

        try { test("/", "/", 5, false); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }

        try { test("/.../a", "/.../a", 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }

        try { test("/a/../../b/../c//.//", "/c", 7, true); }
        catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }

        try { test("/abc/def/ghi/../jkl/../../mno", "/abc/mno", 8, true); }
        catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }

        try { test("/././././.", "/", 9, true); }
        catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }

        try { test("/a/b/c", "/a/b/c", 10, true); }
        catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    string simplifyPath(string path) {
        // Write your code here — use a stack
        return "";
    }
};
// USER_CODE_END

void test(string path, string expected, int tc, bool hidden = false) {
    string got = CodeCoder().simplifyPath(path);
    if (got == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else if (hidden) {
        cout << "TC:" << tc << ":FAIL:hidden\n";
    } else {
        cout << "TC:" << tc << ":FAIL:path=" << path
             << ":expected=" << expected << ":got=" << got << "\n";
    }
}

int main() {
    try { test("/home/", "/home", 1); } catch (...) { cout << "TC:1:FAIL:hidden\n"; }
    try { test("/../", "/", 2); } catch (...) { cout << "TC:2:FAIL:hidden\n"; }
    try { test("/home//foo/", "/home/foo", 3); } catch (...) { cout << "TC:3:FAIL:hidden\n"; }
    try { test("/a/./b/../../c/", "/c", 4); } catch (...) { cout << "TC:4:FAIL:hidden\n"; }
    try { test("/", "/", 5); } catch (...) { cout << "TC:5:FAIL:hidden\n"; }
    try { test("/.../a", "/.../a", 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\n"; }
    try { test("/a/../../b/../c//.//", "/c", 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\n"; }
    try { test("/abc/def/ghi/../jkl/../../mno", "/abc/mno", 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\n"; }
    try { test("/././././.", "/", 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\n"; }
    try { test("/a/b/c", "/a/b/c", 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\n"; }
    return 0;
}'''

py_code = r'''# USER_CODE_START
class CodeCoder:
    def simplifyPath(self, path):
        # Write your code here — use a stack
        return ""
# USER_CODE_END

def test(path, expected, tc, hidden=False):
    got = CodeCoder().simplifyPath(path)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:path={path}:expected={expected}:got={got}")

try: test("/home/", "/home", 1)
except: print("TC:1:FAIL:hidden")
try: test("/../", "/", 2)
except: print("TC:2:FAIL:hidden")
try: test("/home//foo/", "/home/foo", 3)
except: print("TC:3:FAIL:hidden")
try: test("/a/./b/../../c/", "/c", 4)
except: print("TC:4:FAIL:hidden")
try: test("/", "/", 5)
except: print("TC:5:FAIL:hidden")
try: test("/.../a", "/.../a", 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test("/a/../../b/../c//.//", "/c", 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test("/abc/def/ghi/../jkl/../../mno", "/abc/mno", 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test("/././././.", "/", 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test("/a/b/c", "/a/b/c", 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code = r'''// USER_CODE_START
function simplifyPath(path) {
    // Write your code here — use a stack
    return "";
}
// USER_CODE_END

function test(path, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = simplifyPath(path);
    if (got === expected) {
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    } else if (hidden) {
        console.log("TC:" + tc + ":FAIL:hidden");
    } else {
        console.log("TC:" + tc + ":FAIL:path=" + path
            + ":expected=" + expected + ":got=" + got);
    }
}

try { test("/home/", "/home", 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test("/../", "/", 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test("/home//foo/", "/home/foo", 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test("/a/./b/../../c/", "/c", 4); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test("/", "/", 5); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test("/.../a", "/.../a", 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }
try { test("/a/../../b/../c//.//", "/c", 7, true); } catch (e) { console.log("TC:7:FAIL:hidden"); }
try { test("/abc/def/ghi/../jkl/../../mno", "/abc/mno", 8, true); } catch (e) { console.log("TC:8:FAIL:hidden"); }
try { test("/././././.", "/", 9, true); } catch (e) { console.log("TC:9:FAIL:hidden"); }
try { test("/a/b/c", "/a/b/c", 10, true); } catch (e) { console.log("TC:10:FAIL:hidden"); }'''

c_code = r'''#include <stdio.h>
#include <string.h>
#include <stdlib.h>

// USER_CODE_START
void simplifyPath(char* path, char* result) {
    // Write your code here — store the simplified path in 'result'
    // Assume 'result' has enough space (same length as path)
    result[0] = '\0';
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

void runTest(char* path, char* expected, int tc, int hidden) {
    char result[3005];
    simplifyPath(path, result);
    if (strcmp(result, expected) == 0) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\n", tc);
        else printf("TC:%d:FAIL:path=%s:expected=%s:got=%s\n", tc, path, expected, result);
    }
}

int main() {
    runTest("/home/", "/home", 1, 0);
    runTest("/../", "/", 2, 0);
    runTest("/home//foo/", "/home/foo", 3, 0);
    runTest("/a/./b/../../c/", "/c", 4, 0);
    runTest("/", "/", 5, 0);
    runTest("/.../a", "/.../a", 6, 1);
    runTest("/a/../../b/../c//.//", "/c", 7, 1);
    runTest("/abc/def/ghi/../jkl/../../mno", "/abc/mno", 8, 1);
    runTest("/././././.", "/", 9, 1);
    runTest("/a/b/c", "/a/b/c", 10, 1);
    return 0;
}'''

for lang, code in [
    ("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code),
    ("JAVASCRIPT", js_code), ("C", c_code),
]:
    cur.execute(
        "INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) "
        "VALUES (%s, %s, %s, NOW(), NOW())",
        (pid, lang, code)
    )
conn.commit()

cur.execute(
    "SELECT language, LENGTH(solution_template) FROM code_snippets WHERE problem_id = %s ORDER BY language",
    (pid,)
)
for lang, size in cur.fetchall():
    print(f"  {lang}: {size} bytes")

print(f"\nSimplify Path (pid={pid}) — done!")
cur.close()
conn.close()
