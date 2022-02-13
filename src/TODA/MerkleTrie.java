import java.util.*;

public class MerkleTrie {
    static class TrieNode {
        TrieNode branch[] = new TrieNode[2];
        String prefix; // the branch prefix from its parent to the node, empty string for root
        String value;
    }

    TrieNode MerkleRoot;

    private static String getHash(String value) {
        return value;
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

    protected static TrieNode mergeNodes(Pair<TrieNode, Pair<String, Integer>> data0, Pair<TrieNode, Pair<String, Integer>> data1, int lcp) {
        TrieNode par = new TrieNode();
        par.branch[0] = data0.key;
        par.branch[1] = data1.key;
        par.branch[0].prefix = data0.value.key.substring(lcp, data0.value.value);
        par.branch[1].prefix = data1.value.key.substring(lcp, data1.value.value);
        par.value = getHash(data0.value.key + data0.key.value + data1.value.key + data1.key.value);
        return par;
    }

    protected static TrieNode createMerkleTrie(ArrayList<Pair<String, String>> data) {
        /*
        1. Rows are retrieved sorted from file id
        2. Find two adjacent rows whose bit strings share the longest common
           prefix
        3. Merge those two rows, resulting in a single row that associates the lesser
        of the two file ids with H(File1,Val1,File2,Val2)
        4. Repeat steps two and three until a single file-value pair remains
        5. Finally, calculate H(File,Value)
         */

        // TODO: forced branch at bit 1 at end of creating trie by embeding NULL in non-existing branch of root
        int npairs = data.size();
        if (npairs == 1) {
            int chr = 49 - data.get(0).key.charAt(0);
            data.add(new Pair(Integer.toString(chr), null));
            ++ npairs;
        }

        Stack<Pair<TrieNode, Pair<String, Integer>>> nodes = new Stack();
        // create leaves
        for (int i = npairs-1; i >= 0; -- i) {
            TrieNode node = new TrieNode();
            node.branch[0] = null;
            node.branch[1] = null;
            node.value = data.get(i).value;
            nodes.push(new Pair(node, new Pair(data.get(i).key, data.get(i).key.length())));
        }
        TrieNode root = new TrieNode();
        int prev_lcp = getLCP(data.get(0).key, data.get(1).key);
        for (int i = 0; i < npairs - 1; ++ i) {
            Pair<TrieNode, Pair<String, Integer>> data0 = nodes.pop();
            Pair<TrieNode, Pair<String, Integer>> data1 = nodes.pop();
            if (i == npairs - 2) {
                if (prev_lcp == 0) {
                    root = mergeNodes(data0, data1, prev_lcp);
                } else {
                    TrieNode[] root_son = new TrieNode[2];
                    String s0, s1;
                    if (data0.value.key.charAt(0) == '0') {
                        s0 = data0.value.key;
                        s1 = new String("1");
                    } else {
                        s1 = data0.value.key;
                        s0 = new String("0");
                    }
                    root_son[data0.value.key.charAt(0)-'0'] = mergeNodes(data0, data1, prev_lcp);
                    root_son['1'-data0.value.key.charAt(0)] = new TrieNode(); // check if valid null encoding wrt TODA
                    root = mergeNodes(new Pair(root_son[0], new Pair(s0, 1)), new Pair(root_son[1], new Pair(s1, 1)), 0);
                }
                break;
            }
            Pair<TrieNode, Pair<String, Integer>> data2 = nodes.peek();
            int next_lcp = getLCP(data1.value.key, data2.value.key);
            if (prev_lcp > next_lcp) {
                // update element at position i+1 to be i
                nodes.push(new Pair(mergeNodes(data0, data1, prev_lcp), new Pair(data0.value.key, prev_lcp)));
                // new value for prev_lcp = lcp(0, 2) = min(lcp(0, 1), lcp(1, 2)) = lcp(1, 2);
                prev_lcp = next_lcp;
            } else {
                nodes.pop();
                nodes.push(new Pair(mergeNodes(data1, data2, next_lcp), new Pair(data1.value.key, next_lcp)));
                nodes.push(data0);
            }
        }
        return root;
    }

    public static String findValueForAddress(String address, TrieNode node) {
        int index = 0;
        while (node != null && node.branch[0] != null && node.branch[1] != null) {
            node = node.branch[address.charAt(index)-48];
            index += node.prefix.length();
        }

        return node.value;
    }
}
