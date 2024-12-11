/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hedera.block.tools.commands.record2blocks.model;

import java.util.HexFormat;

/**
 * SignatureFile represents a Hedera record file signature file.
 * The below table describes the content that can be parsed from a record signature file.
 *
 * <table>
 * <caption>Signature File Format</caption>
 * <thead>
 * <tr>
 * <th><strong>Name</strong></th>
 * <th><strong>Type (Bytes)</strong></th>
 * <th><strong>Description</strong></th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>File Hash Marker</td>
 * <td>byte</td>
 * <td>Value: 4</td>
 * </tr>
 * <tr>
 * <td>File Hash</td>
 * <td>byte[48]</td>
 * <td>SHA384 hash of corresponding *.rcd file</td>
 * </tr>
 * <tr>
 * <td>Signature Marker</td>
 * <td>byte</td>
 * <td>Value: 3</td>
 * </tr>
 * <tr>
 * <td>Length of Signature</td>
 * <td>int (4)</td>
 * <td>Byte size of the following signature bytes</td>
 * </tr>
 * <tr>
 * <td>Signature</td>
 * <td>byte[]</td>
 * <td>Signature bytes</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @param fileHash SHA384 hash of corresponding *.rcd file
 * @param signature Signature bytes or RSA signature of the file hash, signed by the node's private key
 */
public record SignatureFile(byte[] fileHash, byte[] signature) {
    public static final byte FILE_HASH_MARKER = 4;
    public static final byte SIGNATURE_MARKER = 3;

    /**
     * toString for debugging, prints the file hash and signature in hex format.
     *
     * @return the string representation of the SignatureFile
     */
    @Override
    public String toString() {
        final HexFormat hexFormat = HexFormat.of();
        return "SignatureFile[" + "fileHash="
                + hexFormat.formatHex(fileHash) + ", signature="
                + hexFormat.formatHex(signature) + ']';
    }

    /**
     * Parse a SignatureFile from a byte array.
     *
     * @param bytes the byte array to parse
     * @return the parsed SignatureFile
     */
    public static SignatureFile parse(byte[] bytes) {
        int index = 0;
        if (bytes[index++] != FILE_HASH_MARKER) {
            throw new IllegalArgumentException("Invalid file hash marker");
        }
        final byte[] fileHash = new byte[48];
        System.arraycopy(bytes, index, fileHash, 0, fileHash.length);
        index += fileHash.length;
        if (bytes[index++] != SIGNATURE_MARKER) {
            throw new IllegalArgumentException("Invalid signature marker");
        }
        final int signatureLength = (bytes[index++] & 0xFF) << 24
                | (bytes[index++] & 0xFF) << 16
                | (bytes[index++] & 0xFF) << 8
                | (bytes[index++] & 0xFF);
        final byte[] signature = new byte[signatureLength];
        System.arraycopy(bytes, index, signature, 0, signature.length);
        return new SignatureFile(fileHash, signature);
    }
}
