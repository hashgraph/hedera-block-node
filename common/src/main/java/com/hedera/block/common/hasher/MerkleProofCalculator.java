package com.hedera.block.common.hasher;

import com.hedera.pbj.runtime.io.buffer.Bytes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MerkleProofCalculator {

    public static int indexOfByteArray(List<Bytes> list, Bytes target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(target)) {
                return i;
            }
        }
        return -1;
    }

    public List<MerkleProofElement> calculateBlockMerkleProof(BlockMerkleTreeInfo blockMerkleTreeInfo, Bytes leafHash) {

        // is the leaf in the input tree
        // all leaf hashes are at the lowest level = 0
        int leafIndex = indexOfByteArray(blockMerkleTreeInfo.inputsMerkleTree().getFirst(), leafHash);
        boolean isInputLeaf = leafIndex != -1;
        // is the leaf in the output tree
        if(leafIndex == -1) {
            leafIndex = indexOfByteArray(blockMerkleTreeInfo.outputsMerkleTree().getFirst(), leafHash);
            isInputLeaf = false;
        }

        // if there is no match
        if(leafIndex == -1) {
            return null;
        }

        // if there is a match
        List<MerkleProofElement> proof = new ArrayList<>();
        // get proof elements up to the root of the subtree
        if(isInputLeaf) {
            proof.addAll(calculateMerkleProof(blockMerkleTreeInfo.inputsMerkleTree(), leafIndex));
            //proof.add(new MerkleProofElement(blockMerkleTreeInfo.outputsMerkleTree().getLast().getFirst(), false));
        } else {
            proof.addAll(calculateMerkleProof(blockMerkleTreeInfo.outputsMerkleTree(), leafIndex));
            //proof.add(new MerkleProofElement(blockMerkleTreeInfo.inputsMerkleTree().getLast().getFirst(), false));
        }

        // get proof elements from the root of the subtree to the root of the blockRootHash
        // the last levels of the tree are like this:
        // leftSide: combine(previousBlockHash, inputsRootHash)
        // rightSide: combine(outputsRootHash, stateRootHash)
        if(isInputLeaf) {
            proof.add(new MerkleProofElement(blockMerkleTreeInfo.previousBlockHash(), true));
            Bytes sibling = HashingUtilities.combine(blockMerkleTreeInfo.outputsMerkleTree().getLast().getFirst(), blockMerkleTreeInfo.stateRootHash());
            proof.add(new MerkleProofElement(sibling, false));
        } else {
           proof.add(new MerkleProofElement(blockMerkleTreeInfo.stateRootHash(), false));
            Bytes sibling = HashingUtilities.combine(blockMerkleTreeInfo.previousBlockHash(), blockMerkleTreeInfo.inputsMerkleTree().getLast().getFirst());
            proof.add(new MerkleProofElement(sibling, true));
        }

        return new ArrayList<>(proof);
    }

    // Calculate the Merkle root hash of a list of leaf hashes.
    // Requires the completeMerkleTree to be a list of lists of byte arrays, where each list represents a level of the tree, fully padded.
    public List<MerkleProofElement> calculateMerkleProof(List<List<Bytes>> completeMerkleTree, int leafIndex) {
        List<MerkleProofElement> proof = new ArrayList<>();
        int index = leafIndex;

        // Iterate over each level except the root.
        for(int level = 0; level < completeMerkleTree.size() - 1; level++) {
            List<Bytes> levelHashes = completeMerkleTree.get(level);
            if(index % 2 == 0) {
                // If the index is even, the sibling is the next hash in the list.
                proof.add(new MerkleProofElement(levelHashes.get(index + 1), false));
            } else {
                // If the index is odd, the sibling is the previous hash in the list.
                proof.add(new MerkleProofElement(levelHashes.get(index - 1), true));
            }
            // Move up to the parent level.
            index /= 2;
        }

        return proof;
    }

    /**
     * Verifies a Merkle proof for a given leaf hash.
     *
     * @param proof    A list of MerkleProofElement that include sibling hashes and whether they're on the left.
     * @param leafHash The hash (as a byte array) of the leaf node.
     * @param rootHash The expected Merkle root hash.
     * @return true if the proof is valid and the computed root matches rootHash; false otherwise.
     */
    public boolean verifyMerkleProof(List<MerkleProofElement> proof, Bytes leafHash, Bytes rootHash) {
        Bytes computedHash = leafHash;
        for (MerkleProofElement element : proof) {
            if (element.isLeft()) {
                // Sibling is on the left: concatenate sibling hash + computed hash.
                computedHash = HashingUtilities.combine(element.hash(), computedHash);
            } else {
                // Sibling is on the right: concatenate computed hash + sibling hash.
                computedHash = HashingUtilities.combine(computedHash, element.hash());
            }
        }
        return computedHash.equals(rootHash);
    }

}
