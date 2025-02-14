package com.hedera.block.common.hasher;

import java.util.ArrayList;
import java.util.List;

public class MerkleProofCalculator {

    // Calculate the Merkle root hash of a list of leaf hashes.
    // Requires the completeMerkleTree to be a list of lists of byte arrays, where each list represents a level of the tree, fully padded.
    public List<byte[]> calculateMerkleProof(List<List<byte[]>> completeMerkleTree, int leafIndex) {
        List<byte[]> proof = new ArrayList<>();
        int index = leafIndex;

        // Iterate over each level except the root.
        for(int level = 0; level < completeMerkleTree.size() - 1; level++) {
            List<byte[]> levelHashes = completeMerkleTree.get(level);
            if(index % 2 == 0) {
                // If the index is even, the sibling is the next hash in the list.
                proof.add(levelHashes.get(index + 1));
            } else {
                // If the index is odd, the sibling is the previous hash in the list.
                proof.add(levelHashes.get(index - 1));
            }
            // Move up to the parent level.
            index /= 2;
        }

        return proof;
    }

    public int indexIfExist(List<byte[]> leafHashes, byte[] leafHash) {
        return leafHashes.indexOf(leafHash);
    }

}
