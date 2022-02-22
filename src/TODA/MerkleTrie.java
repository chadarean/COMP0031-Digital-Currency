package src.TODA; 

import java.util.*;

import javax.management.RuntimeErrorException;

public class MerkleTrie {
    public final static int ADDRESS_SIZE = 256;

    public static class StackNode {
        int pLcp;
        TrieNode tNode;
        String addr;
        int prefLen;
        public StackNode(int pLcp, TrieNode tNode, String addr, int prefLen) {
            this.pLcp = pLcp;
            this.tNode = tNode;
            this.addr = addr;
            this.prefLen = prefLen;
        } 
    }

    public static class TrieNode {
        TrieNode branch[] = new TrieNode[2];
        String prefix; // the branch prefix from its parent to the node, empty string for root
        String value;
        public TrieNode(TrieNode branch0, TrieNode branch1, String value) {
            this.branch[0] = branch0;
            this.branch[1] = branch1;
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

    public static TrieNode mergeNodes(StackNode lnode, StackNode rnode) {
        int lcp = rnode.pLcp;
        System.out.println("Merging " + lnode.addr + " with " + rnode.addr);
        System.out.println("lcp=" + Integer.toString(lcp) + "and l1=" + Integer.toString(lnode.prefLen) + " l2=" +Integer.toString(rnode.prefLen));
        
        String parValue = Utils.getHash(lnode.addr.substring(lcp, lnode.prefLen) + lnode.tNode.value + 
        rnode.addr.substring(lcp, rnode.prefLen) + rnode.tNode.value);
        TrieNode par = new TrieNode(lnode.tNode, rnode.tNode, parValue);
        par.branch[0].prefix = lnode.addr.substring(lcp, lnode.prefLen);
        par.branch[1].prefix = rnode.addr.substring(lcp, rnode.prefLen);
        System.out.println("val = " + par.value);
        return par;
    }

    public static TrieNode createMerkleTrie(ArrayList<Pair<String, String>> data) {
        /*
        1. Rows are retrieved sorted from file id
        2. Find two adjacent rows whose bit strings share the longest common
           prefix
        3. Merge those two rows, resulting in a single row that associates the lesser
        of the two file ids with H(File1,Val1,File2,Val2)
        4. Repeat steps two and three until a single file-value pair remains
        5. Finally, calculate H(File,Value)
         */

        /*
        pseudocode: 1. create initail stack2 with nodes = StackNode{lcp(prev_node, crt_node), node, data_i.key(), data_i.key().length}
        // TODO: what lcp to add for first node
        stack1 = first pair {0, 1}
        2. while (stack1 != empty || stack2 != empty) 
             while (!stack2.empty() && stack1.peek().lcp < stack2.peek()) {
               stack1.push(stack2.pop());
             }
             if (!stack1.empty()) // is this necessary
             merge the 2 nodes of top of stack1 and remove top value
             update lcp at stack2
        */

        int npairs = data.size();
        if (npairs == 1 || (data.get(0).key.charAt(0) == data.get(npairs-1).key.charAt(0))) {
            int chr = 49 - data.get(0).key.charAt(0);
            data.add(chr, new Pair(Integer.toString(chr), null)); // TODO: the paper adds address of length=1, prove that it's correct
            ++ npairs;
        }

        Stack<StackNode> stack1 = new Stack();

        // create first 2 leaves and add them to stack1
        for (int i = 0; i < 2; ++ i) {
            TrieNode node = new TrieNode(null, null, data.get(i).value);
            node.branch[0] = null;
            node.branch[1] = null;
            node.value = data.get(i).value; // the node value is the value associated with the i-th key
            Pair<String, String> data_i = data.get(i);
            int pLcp = (i > 0) ? getLCP(data_i.key, data.get(i-1).key) : 0;
            stack1.push(new StackNode(pLcp, node, data_i.key, data_i.key.length()));
        }

        int stack2Top = 2;
        int stack2TopLcp = (stack2Top < npairs) ? getLCP(data.get(stack2Top).key, data.get(1).key) : 0;
        while (stack2Top < npairs || stack1.size() > 1) {
            System.out.println("Stack2top = " + Integer.toString(stack2Top) + "lcp2top = " + Integer.toString(stack2TopLcp));
            while (stack2Top < npairs && (stack1.size() <= 1 || stack1.peek().pLcp <= stack2TopLcp)) {
                Pair<String, String> stack2TopData = data.get(stack2Top);
                stack1.push(new StackNode(stack2TopLcp, new TrieNode(null, null, stack2TopData.value), stack2TopData.key, stack2TopData.key.length()));
                ++ stack2Top;
                stack2TopLcp = (stack2Top < npairs) ? getLCP(stack2TopData.key, data.get(stack2Top).key) : 0;
            }
            StackNode rnode = stack1.pop();
            StackNode lnode = stack1.pop();
            // merge the 2 nodes at the top of stack1
            stack1.add(new StackNode(lnode.pLcp, mergeNodes(lnode, rnode), lnode.addr, rnode.pLcp)); 
            //stack2TopLcp doesn't change because of the inequality constraint
        }

        return stack1.peek().tNode;
    }

    public static String findValueForAddress(String address, TrieNode node) {
        int index = 0;
        while (node != null && node.branch[0] != null && node.branch[1] != null) {
            node = node.branch[address.charAt(index)-48];
            if (!address.substring(index, index + node.prefix.length()).equals(node.prefix)) {
                return null;
            }
            index += node.prefix.length();
        }
        return node.value;
    }

    public static MerkleProof getMerkleProof(String address, TrieNode node) {
        int index = 0;
        //TODO: prove that MerkleTree construction guarantees no parent will have a null branch: bc
        // the parent will be combined with the non null branch

        //TODO: implement null proofs
        MerkleProof proof = new MerkleProof();
        String dataHash = node.value;
        int num_f = 0;
        int chosen_branch = -1;
    
        while (node != null && node.branch[0] != null && node.branch[1] != null) {
            proof.addFrame(
                    node.branch[0].value, (byte)(node.branch[0].prefix.length()-1), //the prefix length in TODA Frame is prefix length - 1
                    Utils.prefixToBytes(node.branch[0].prefix),
                    node.branch[1].value, (byte)(node.branch[1].prefix.length()-1),
                    Utils.prefixToBytes(node.branch[1].prefix),
                    dataHash);
            dataHash = null;
            
            if (proof.null_proof) {
                if (chosen_branch != 0 && chosen_branch != 1) {
                    throw new RuntimeException("Error constructing null proof");
                }
                node = node.branch[chosen_branch];
            } else {
                int expBranch = address.charAt(index)-48;
                int cmp = address.substring(index, index+node.branch[expBranch].prefix.length()).
                   compareTo(node.branch[expBranch].prefix);
                if (cmp == 0) {
                    node = node.branch[expBranch];
                } else {
                    if (cmp < 0) {
                    node = node.branch[expBranch];
                    chosen_branch = 0;
                    proof.null_proof = true;
                } else {
                    node = node.branch[1];
                    chosen_branch = expBranch;
                    proof.null_proof = true;
                }
            }
            }
    
            index += node.prefix.length();
            num_f += 1;
        }
        if (index < address.length()) {
            proof.null_proof = true;
        }
        // if (proof.null_proof) {
        //     System.out.println("Null proof for address=" + address);
        // }
        //System.out.println("Added nf=" + Integer.toString(num_f) + " for address=" + address + "; inedx = " + Integer.toString(index));
        return proof;
    }
}
