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

package com.hedera.block.tools.commands.record2blocks.mirrornode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.HexFormat;

/**
 * Query Mirror Node and fetch block information
 */
public class FetchBlockQuery {

    /**
     * Get the record file name for a block number from the mirror node.
     *
     * @param blockNumber the block number
     * @return the record file name
     */
    public static String getRecordFileNameForBlock(long blockNumber) {
        final String url = "https://mainnet-public.mirrornode.hedera.com/api/v1/blocks/" + blockNumber;
        final JsonObject json = readUrl(url);
        return json.get("name").getAsString();
    }

    /**
     * Get the previous hash for a block number from the mirror node.
     *
     * @param blockNumber the block number
     * @return the record file name
     */
    public static Bytes getPreviousHashForBlock(long blockNumber) {
        final String url = "https://mainnet-public.mirrornode.hedera.com/api/v1/blocks/" + blockNumber;
        final JsonObject json = readUrl(url);
        final String hashStr = json.get("previous_hash").getAsString();
        return Bytes.wrap(HexFormat.of().parseHex(hashStr.substring(2))); // remove 0x prefix and parse
    }

    /**
     * Read a URL and return the JSON object.
     *
     * @param url the URL to read
     * @return the JSON object
     */
    private static JsonObject readUrl(String url) {
        try {
            URL u = new URI(url).toURL();
            try (Reader reader = new InputStreamReader(u.openStream())) {
                return new Gson().fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test main method
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Fetching block query...");
        int blockNumber = 69333000;
        System.out.println("blockNumber = " + blockNumber);
        String recordFileName = getRecordFileNameForBlock(blockNumber);
        System.out.println("recordFileName = " + recordFileName);
    }
}