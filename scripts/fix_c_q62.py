import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

c_code = '''#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct TreeNode{int val;struct TreeNode *left,*right;};

// USER_CODE_START
void levelOrder(struct TreeNode* root, char* out) {
    // Write your code here — BFS level by level
    // Store each level as "val1 val2 ... ," (comma separates levels)
    out[0] = '\\0';
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

struct TreeNode* build(int** a, int n) {
    if (!n || !a || !a[0]) return NULL;
    struct TreeNode** q = malloc(n * sizeof(struct TreeNode*));
    int front = 0, rear = 0;
    struct TreeNode* r = malloc(sizeof(struct TreeNode));
    r->val = *a[0]; r->left = r->right = NULL;
    q[rear++] = r;
    int i = 1;
    while (front < rear && i < n) {
        struct TreeNode* cur = q[front++];
        if (a[i]) {
            struct TreeNode* l = malloc(sizeof(struct TreeNode));
            l->val = *a[i]; l->left = l->right = NULL;
            cur->left = l; q[rear++] = l;
        }
        i++;
        if (i < n && a[i]) {
            struct TreeNode* ri = malloc(sizeof(struct TreeNode));
            ri->val = *a[i]; ri->left = ri->right = NULL;
            cur->right = ri; q[rear++] = ri;
        }
        i++;
    }
    free(q);
    return r;
}

void runTest(int** a, int n, char* expected, int tc, int hidden) {
    struct TreeNode* root = build(a, n);
    char out[20000] = {0};
    levelOrder(root, out);
    if (strcmp(out, expected) == 0) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:got=%s:exp=%s\\n", tc, out, expected);
    }
}

int main() {
    int _3=3,_9=9,_20=20,_15=15,_7=7,_1=1,_2=2,_4=4,_5=5,_6=6,_0=0,_8=8,_10=10,_11=11,_12=12,_13=13,_14=14,_15_=15;

    int* t1[] = {&_3,&_9,&_20,NULL,NULL,&_15,&_7};
    runTest(t1, 7, "3 ,9 20 ,15 7 ,", 1, 0);

    int* t2[] = {&_1};
    runTest(t2, 1, "1 ,", 2, 0);

    runTest(NULL, 0, "", 3, 0);

    int* t4[] = {&_1,&_2,&_3,&_4,&_5,&_6,&_7};
    runTest(t4, 7, "1 ,2 3 ,4 5 6 7 ,", 4, 0);

    int* t5[] = {&_1,&_2,NULL,&_3,NULL,&_4};
    runTest(t5, 6, "1 ,2 ,3 ,4 ,", 5, 0);

    int* t6[] = {&_0,&_0,&_0,&_0,&_0,&_0,&_0};
    runTest(t6, 7, "0 ,0 0 ,0 0 0 0 ,", 6, 1);

    int* t7[] = {&_1,&_2,&_3,NULL,NULL,&_4,&_5};
    runTest(t7, 7, "1 ,2 3 ,4 5 ,", 7, 1);

    int* t8[] = {&_1,&_2,&_2,&_3,&_3,NULL,NULL,&_4,&_4};
    runTest(t8, 9, "1 ,2 2 ,3 3 ,4 4 ,", 8, 1);

    int* t9[] = {&_1,NULL,&_2,NULL,&_3};
    runTest(t9, 5, "1 ,2 ,3 ,", 9, 1);

    int* t10[] = {&_1,&_2,&_3,&_4,&_5,&_6,&_7,&_8,&_9,&_10,&_11,&_12,&_13,&_14,&_15_};
    runTest(t10, 15, "1 ,2 3 ,4 5 6 7 ,8 9 10 11 12 13 14 15 ,", 10, 1);

    return 0;
}'''

cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(149,'C',%s,NOW(),NOW())",(c_code,))
conn.commit()
print("C harness fixed for pid=149!")
cur.close(); conn.close()
