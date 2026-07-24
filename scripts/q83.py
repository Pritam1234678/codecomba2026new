"""
Clone Graph
=============
Given a reference of a node in a connected undirected graph, return a deep copy
(clone) of the graph. Each node contains a value (int) and a list of neighbors.

Test cases use adjacency list format and validate structure equality.

Approach: DFS with a visited map (original node → cloned node).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Clone Graph"
desc=(
    "Given a reference of a node in a connected undirected graph, return a deep copy of the graph.\n\n"
    "Each node in the graph contains:\n"
    "- int val\n"
    "- List of references to its neighbor nodes\n\n"
    "The graph is undirected, so if node A has neighbor B, then B also has neighbor A.\n"
    "The input is given as an adjacency list, but your function receives a single node reference.\n\n"
    "Use DFS with a hash map to track visited/original→cloned mappings. For each neighbor,\n"
    "if not yet cloned, recursively clone it first, then add to current node's neighbors."
)
infmt="First line contains number of nodes N.\nThen N lines follow, each containing neighbors (space-separated) for nodes 1 to N.\nStarting node is 1.\n-1 means no neighbors."
outfmt="Validates clone structurally — output format is irrelevant as driver checks."
cons="1 ≤ nodes ≤ 100\nNode.val is unique.\nGraph is connected and undirected."
e1="Input:\n4\n2 4\n1 3\n2 4\n1 3\n\nOutput:\nCloned graph matches original."
e2="Input:\n1\n-1\n\nOutput:\nSingle node clone."
e3="Input:\n2\n2\n1\n\nOutput:\nTwo node connected graph."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Graph, Hash Table, DFS, BFS",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
// class Node {
//     public int val;
//     public List<Node> neighbors;
//     public Node() { val = 0; neighbors = new ArrayList<Node>(); }
//     public Node(int _val) { val = _val; neighbors = new ArrayList<Node>(); }
//     public Node(int _val, List<Node> _neighbors) { val = _val; neighbors = _neighbors; }
// }
class CodeCoder {
    public Node cloneGraph(Node node) {
        // Write your code here — DFS with visited map
        return null;
    }
}
// USER_CODE_END

class Node {
    public int val;
    public List<Node> neighbors;
    public Node() { val = 0; neighbors = new ArrayList<Node>(); }
    public Node(int _val) { val = _val; neighbors = new ArrayList<Node>(); }
    public Node(int _val, List<Node> _neighbors) { val = _val; neighbors = _neighbors; }
}

public class Main {
    static Node build(int[][] adj) {
        if (adj == null || adj.length == 0) return null;
        Node[] nodes = new Node[adj.length + 1];
        for (int i = 1; i <= adj.length; i++) nodes[i] = new Node(i);
        for (int i = 1; i <= adj.length; i++) {
            for (int n : adj[i-1]) {
                if (n == -1) break;
                nodes[i].neighbors.add(nodes[n]);
            }
        }
        return nodes[1];
    }
    static boolean graphEq(Node a, Node b, Set<Integer> visited) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.val != b.val) return false;
        if (a == b) return true;
        if (visited.contains(a.val)) return true;
        visited.add(a.val);
        if (a.neighbors.size() != b.neighbors.size()) return false;
        Set<Integer> aSet = new HashSet<>(), bSet = new HashSet<>();
        for (Node n : a.neighbors) aSet.add(n.val);
        for (Node n : b.neighbors) bSet.add(n.val);
        if (!aSet.equals(bSet)) return false;
        for (int i = 0; i < a.neighbors.size(); i++) {
            if (!graphEq(a.neighbors.get(i), b.neighbors.get(i), visited)) return false;
        }
        return true;
    }
    static void test(int[][] adj, int tc, boolean h) {
        Node orig = build(adj);
        Node clone = new CodeCoder().cloneGraph(orig);
        if (graphEq(orig, clone, new HashSet<>()))
            System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:graph not equal");
    }
    public static void main(String[] a) {
        try { test(new int[][]{{2,4},{1,3},{2,4},{1,3}}, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[][]{{-1}}, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[][]{{2},{1}}, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[][]{{2,3},{1,3},{1,2}}, 4, false); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[][]{{2},{3},{1}}, 5, false); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[][]{{2,3,4,5},{1,3,4,5},{1,2,4,5},{1,2,3,5},{1,2,3,4}}, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
        try { test(new int[][]{{2},{1},{4},{3}}, 7, true); } catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }
        try { test(new int[][]{{2,4,6},{1,3,5},{2,4,6},{1,3,5},{2,4,6},{1,3,5}}, 8, true); } catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }
        try { test(new int[][]{{2},{1},{2},{1}}, 9, true); } catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }
        try { test(new int[][]{{2,3},{1,4},{1,4},{2,3}}, 10, true); } catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
// class Node {
// public:
//     int val;
//     vector<Node*> neighbors;
//     Node() { val = 0; neighbors = vector<Node*>(); }
//     Node(int _val) { val = _val; neighbors = vector<Node*>(); }
//     Node(int _val, vector<Node*> _neighbors) { val = _val; neighbors = _neighbors; }
// };
class CodeCoder {
public:
    Node* cloneGraph(Node* node) {
        // Write your code here — DFS with map
        return NULL;
    }
};
// USER_CODE_END

class Node {
public:
    int val;
    vector<Node*> neighbors;
    Node() { val = 0; neighbors = vector<Node*>(); }
    Node(int _val) { val = _val; neighbors = vector<Node*>(); }
    Node(int _val, vector<Node*> _neighbors) { val = _val; neighbors = _neighbors; }
};

Node* build(vector<vector<int>> adj) {
    if (adj.empty()) return NULL;
    vector<Node*> nodes(adj.size() + 1, NULL);
    for (int i = 1; i <= (int)adj.size(); i++) nodes[i] = new Node(i);
    for (int i = 1; i <= (int)adj.size(); i++) {
        for (int n : adj[i-1]) {
            if (n == -1) break;
            nodes[i]->neighbors.push_back(nodes[n]);
        }
    }
    return nodes[1];
}
bool grEq(Node* a, Node* b, unordered_set<int>& v) {
    if (!a && !b) return true;
    if (!a || !b) return false;
    if (a->val != b->val) return false;
    if (a == b) return true;
    if (v.count(a->val)) return true;
    v.insert(a->val);
    if (a->neighbors.size() != b->neighbors.size()) return false;
    unordered_set<int> as, bs;
    for (auto n : a->neighbors) as.insert(n->val);
    for (auto n : b->neighbors) bs.insert(n->val);
    if (as != bs) return false;
    for (size_t i = 0; i < a->neighbors.size(); i++) {
        if (!grEq(a->neighbors[i], b->neighbors[i], v)) return false;
    }
    return true;
}
void test(vector<vector<int>> adj, int tc, bool h = false) {
    Node* o = build(adj);
    Node* c = CodeCoder().cloneGraph(o);
    unordered_set<int> vis;
    if (grEq(o, c, vis)) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL\\n";
}
int main() {
    try { test({{2,4},{1,3},{2,4},{1,3}}, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({{-1}}, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({{2},{1}}, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({{2,3},{1,3},{1,2}}, 4); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({{2},{3},{1}}, 5); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({{2,3,4,5},{1,3,4,5},{1,2,4,5},{1,2,3,5},{1,2,3,4}}, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { test({{2},{1},{4},{3}}, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { test({{2,4,6},{1,3,5},{2,4,6},{1,3,5},{2,4,6},{1,3,5}}, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { test({{2},{1},{2},{1}}, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { test({{2,3},{1,4},{1,4},{2,3}}, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
# class Node:
#     def __init__(self, val=0, neighbors=None):
#         self.val = val
#         self.neighbors = neighbors if neighbors is not None else []
class CodeCoder:
    def cloneGraph(self, node):
        return None
# USER_CODE_END

class Node:
    def __init__(self, val=0, neighbors=None):
        self.val = val
        self.neighbors = neighbors if neighbors is not None else []

def build(adj):
    if not adj: return None
    nodes=[None]+[Node(i) for i in range(1,len(adj)+1)]
    for i,row in enumerate(adj,1):
        for n in row:
            if n==-1:break
            nodes[i].neighbors.append(nodes[n])
    return nodes[1]

def grEq(a,b,vis):
    if not a and not b: return True
    if not a or not b: return False
    if a.val!=b.val: return False
    if a is b: return True
    if a.val in vis: return True
    vis.add(a.val)
    if len(a.neighbors)!=len(b.neighbors): return False
    aSet={n.val for n in a.neighbors}
    bSet={n.val for n in b.neighbors}
    if aSet!=bSet: return False
    for i in range(len(a.neighbors)):
        if not grEq(a.neighbors[i],b.neighbors[i],vis): return False
    return True

def test(adj,tc,h=False):
    o=build(adj)
    c=CodeCoder().cloneGraph(o)
    if grEq(o,c,set()): print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL")

try:test([[2,4],[1,3],[2,4],[1,3]],1)
except:print("TC:1:FAIL:hidden")
try:test([[-1]],2)
except:print("TC:2:FAIL:hidden")
try:test([[2],[1]],3)
except:print("TC:3:FAIL:hidden")
try:test([[2,3],[1,3],[1,2]],4)
except:print("TC:4:FAIL:hidden")
try:test([[2],[3],[1]],5)
except:print("TC:5:FAIL:hidden")
try:test([[2,3,4,5],[1,3,4,5],[1,2,4,5],[1,2,3,5],[1,2,3,4]],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[2],[1],[4],[3]],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[2,4,6],[1,3,5],[2,4,6],[1,3,5],[2,4,6],[1,3,5]],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[2],[1],[2],[1]],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[2,3],[1,4],[1,4],[2,3]],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class Node {
//     constructor(val, neighbors) {
//         this.val = val === undefined ? 0 : val;
//         this.neighbors = neighbors === undefined ? [] : neighbors;
//     }
// }
function cloneGraph(node) {
    return null;
}
// USER_CODE_END

class Node {
    constructor(val, neighbors) {
        this.val = val === undefined ? 0 : val;
        this.neighbors = neighbors === undefined ? [] : neighbors;
    }
}

function build(adj){
    if(!adj||!adj.length)return null;
    const nodes=[null];
    for(let i=1;i<=adj.length;i++)nodes.push(new Node(i));
    for(let i=1;i<=adj.length;i++){
        if(adj[i-1][0]===-1)break;
        for(const n of adj[i-1])nodes[i].neighbors.push(nodes[n]);
    }
    return nodes[1];
}
function grEq(a,b,vis){
    if(!a&&!b)return true;
    if(!a||!b)return false;
    if(a.val!==b.val)return false;
    if(a===b)return true;
    if(vis.has(a.val))return true;
    vis.add(a.val);
    if(a.neighbors.length!==b.neighbors.length)return false;
    const aS=new Set(a.neighbors.map(n=>n.val)),bS=new Set(b.neighbors.map(n=>n.val));
    if(JSON.stringify([...aS].sort())!==JSON.stringify([...bS].sort()))return false;
    for(let i=0;i<a.neighbors.length;i++){if(!grEq(a.neighbors[i],b.neighbors[i],vis))return false;}
    return true;
}
function test(adj,tc,h){if(h===undefined)h=false;
    const o=build(adj),c=cloneGraph(o);
    if(grEq(o,c,new Set()))console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL");
}
try{test([[2,4],[1,3],[2,4],[1,3]],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[-1]],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[2],[1]],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[2,3],[1,3],[1,2]],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[2],[3],[1]],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[2,3,4,5],[1,3,4,5],[1,2,4,5],[1,2,3,5],[1,2,3,4]],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[2],[1],[4],[3]],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[2,4,6],[1,3,5],[2,4,6],[1,3,5],[2,4,6],[1,3,5]],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[2],[1],[2],[1]],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[2,3],[1,4],[1,4],[2,3]],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
// struct Node {
//     int val;
//     struct Node** neighbors;
//     int numNeighbors;
// };
struct Node* cloneGraph(struct Node* s) { return NULL; }
// USER_CODE_END

int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
