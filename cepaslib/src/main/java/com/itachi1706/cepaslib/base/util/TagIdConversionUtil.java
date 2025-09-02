/*
 * TagIdConversionUtil.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2024 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.itachi1706.cepaslib.base.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for displaying tag ID conversion process
 */
public class TagIdConversionUtil {

    public static class ConversionStep {
        private final int byteIndex;
        private final byte originalByte;
        private final int unsignedValue;
        private final int withOffset;
        private final String hexString;
        private final String finalHex;

        public ConversionStep(int byteIndex, byte originalByte, int unsignedValue, 
                            int withOffset, String hexString, String finalHex) {
            this.byteIndex = byteIndex;
            this.originalByte = originalByte;
            this.unsignedValue = unsignedValue;
            this.withOffset = withOffset;
            this.hexString = hexString;
            this.finalHex = finalHex;
        }

        public int getByteIndex() { return byteIndex; }
        public byte getOriginalByte() { return originalByte; }
        public int getUnsignedValue() { return unsignedValue; }
        public int getWithOffset() { return withOffset; }
        public String getHexString() { return hexString; }
        public String getFinalHex() { return finalHex; }
    }

    /**
     * Get detailed conversion steps for a tag ID
     */
    @NonNull
    public static List<ConversionStep> getConversionSteps(@NonNull byte[] tagId) {
        List<ConversionStep> steps = new ArrayList<>();
        
        for (int i = 0; i < tagId.length; i++) {
            byte value = tagId[i];
            
            // Step 1: Convert signed byte to unsigned
            int unsignedValue = value & 0xff;
            
            // Step 2: Add 0x100 (256) to ensure 3-digit hex
            int withOffset = unsignedValue + 0x100;
            
            // Step 3: Convert to hex string
            String hexString = Integer.toString(withOffset, 16);
            
            // Step 4: Remove first character to get 2-digit hex
            String finalHex = hexString.substring(1);
            
            steps.add(new ConversionStep(i, value, unsignedValue, withOffset, hexString, finalHex));
        }
        
        return steps;
    }

    /**
     * Get the final hex string result
     */
    @NonNull
    public static String getHexString(@NonNull byte[] tagId) {
        StringBuilder result = new StringBuilder();
        for (byte value : tagId) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    /**
     * Get a formatted description of the conversion process
     */
    @NonNull
    public static String getConversionDescription(@NonNull byte[] tagId) {
        StringBuilder description = new StringBuilder();
        description.append("Raw bytes: ").append(ByteUtils.getHexString(tagId, "ERROR")).append("\n");
        description.append("Length: ").append(tagId.length).append(" bytes\n");
        description.append("Final result: ").append(getHexString(tagId)).append(" (").append(getHexString(tagId).length()).append(" hex characters)");
        return description.toString();
    }

    /**
     * Get a detailed step-by-step conversion explanation
     */
    @NonNull
    public static String getDetailedConversionExplanation(@NonNull byte[] tagId) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("CONVERSION PROCESS:\n");
        explanation.append("For each byte: (byte & 0xff) + 0x100 → hex string → substring(1)\n\n");
        
        List<ConversionStep> steps = getConversionSteps(tagId);
        for (ConversionStep step : steps) {
            explanation.append(String.format("Byte %d: %3d (0x%02X) → unsigned: %3d → +0x100: %3d → hex: %s → final: %s\n",
                step.getByteIndex(), step.getOriginalByte(), step.getOriginalByte(),
                step.getUnsignedValue(), step.getWithOffset(), step.getHexString(), step.getFinalHex()));
        }
        
        explanation.append("\nFinal hex string: ").append(getHexString(tagId));
        return explanation.toString();
    }
}
