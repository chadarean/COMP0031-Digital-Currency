import java.util.*;

public class MerkleTrie {
    static class TrieNode {
        TrieNode branch[] = new TrieNode[2];
        String prefix; // the branch prefix from its parent to the node, empty string for root
    }

    static class Pair<U, V> {
        public final U key; // 2^256 bits address
        public final V value; // hash
        public Pair(U key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private static int getLCP(String a, String b) {
        int max_lcp = Math.min(a.length(), b.length());
        for (int i = 0; i < max_lcp; ++ i) {
            if (a.charAt(i) != b.charAt(i)) {
                return i;
            }
        }
        return max_lcp;
    }

    protected static TrieNode mergeNodes(Pair<TrieNode, String> data0, Pair<TrieNode, String> data1, int lcp) {
        TrieNode par = new TrieNode();
        par.branch[0] = data0.key;
        par.branch[1] = data1.key;
        par.branch[0].prefix = data0.value.substring(lcp, data0.value.length());
        par.branch[1].prefix = data1.value.substring(lcp, data1.value.length());
        return par;
    }

    protected static TrieNode createMerkleTrie(List<Pair<String, String>> data) {
        /*
        1. Rows are retrieved sorted from file id
        2. Find two adjacent rows whose bit strings share the longest common
           prefix, e.g. File1 and File2
           Filei
           Filei+1
           Filei+2
           lcp01 > lcp12 => overwrite Filei on Filei+1, inc i
           lcp01 < lcp12 => overwrite Filei+1 on Filei+2, and Filei on Filei+1
           File2, lcp02 = min(lcp01, lcp12) = lcp12 because we choose max
        3. Merge those two rows, resulting in a single row that associates the lesser
        of the two file ids with H(File1,Val1,File2,Val2)
        4. Repeat steps two and three until a single file-value pair remains
        5. Finally, calculate H(File,Value)
         */

        // TODO: forced branch at bit 1 at end of creating trie by embeding NULL in non-existing branch of root
        int npairs = data.size();
        if (npairs == 1) {
            //TODO: create MT with single element
            return new TrieNode();
        }

        Stack<Pair<TrieNode, String>> nodes = new Stack();
        // create leaves
        for (int i = npairs-1; i >= 0; -- i) {
            TrieNode node = new TrieNode();
            node.branch[0] = null;
            node.branch[1] = null;
            nodes.push(new Pair(node, data.get(i).key));
        }
        TrieNode root = new TrieNode();
        int prev_lcp = getLCP(data.get(0).value, data.get(1).value);
        for (int i = 0; i < npairs - 1; ++ i) {
            Pair<TrieNode, String> data0 = nodes.pop();
            Pair<TrieNode, String> data1 = nodes.pop();
            if (i == npairs - 2) {
                if (prev_lcp == 0) {
                    root = mergeNodes(data0, data1, prev_lcp);
                } else {
                    TrieNode[] root_son = new TrieNode[2];
                    root_son[data0.value.charAt(0)-'0'] = mergeNodes(data0, data1, prev_lcp);
                    root_son['1'-data0.value.charAt(1)] = null; // check if valid null encoding wrt TODA
                    String s0 = new String("0");
                    String s1 = new String("1");
                    root = mergeNodes(new Pair(root_son[0], s0), new Pair(root_son[1], s1), 0);
                }
                break;
            }
            Pair<TrieNode, String> data2 = nodes.peek();
            int next_lcp = getLCP(data1.value, data2.value);
            if (prev_lcp > next_lcp) {
                prev_lcp = next_lcp;
                nodes.push(new Pair(mergeNodes(data0, data1, prev_lcp), data0.value.substring(0, prev_lcp)));
                // update element at position i+1 to be i
            } else {
                nodes.pop();
                nodes.push(new Pair(mergeNodes(data1, data2, next_lcp), data1.value.substring(0, next_lcp)));
                nodes.push(data0);
                // new value for prev_lcp = lcp(0, 2) = min(lcp(0, 1); lcp(1, 2)) = lcp(0, 1);
            }
        }
        return root;
    }

    public static void main(String args[]) {
        TrieNode node = createMerkleTrie(Arrays.asList(new Pair("00", "01")));
    }
}
