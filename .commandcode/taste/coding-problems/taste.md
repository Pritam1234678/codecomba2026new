# coding-problems
- When creating coding problem test harnesses, ALL 5 languages (Java, C++, Python, JavaScript, C) must have real test cases — never hardcode "PASS" outputs or leave any language without working test cases. C harness must actually call the user's function and verify results, not just print PASS. Confidence: 0.90
- When inserting coding problems into the database, create one separate Python file per problem rather than a single batch script for all problems. Confidence: 0.80
- Use class name "CodeCoder" instead of "Solution" in all test harnesses (Java, C++, Python). Confidence: 0.85
- Include exactly 10 test cases in each harness (5 visible + 5 hidden). Confidence: 0.85
- Do NOT include solution hints or tips in the harness comments — only the problem requirement. Confidence: 0.75
- When inserting a coding problem into the DB, also update remaining.md — decrement the remaining count and remove the problem from the table. Confidence: 0.65
- Deploy coding problem scripts to VM using: python3 compile check → scp → ssh run + cleanup (python3 ... && rm ...; echo '===done'). Confidence: 0.70
