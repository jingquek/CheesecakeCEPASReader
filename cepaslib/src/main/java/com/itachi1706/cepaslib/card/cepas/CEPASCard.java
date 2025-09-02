/*
 * CEPASCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2011-2012, 2014, 2016 Eric Butler <eric@codebutler.com>
 * Copyright (C) 2011 Sean Cross <sean@chumby.com>
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

package com.itachi1706.cepaslib.card.cepas;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.itachi1706.cepaslib.base.ui.FareBotUiTree;
import com.itachi1706.cepaslib.base.util.ByteArray;
import com.itachi1706.cepaslib.base.util.ByteUtils;
import com.itachi1706.cepaslib.card.Card;
import com.itachi1706.cepaslib.card.CardType;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@AutoValue
public abstract class CEPASCard extends Card {

    @NonNull
    public static CEPASCard create(
            @NonNull ByteArray tagId,
            @NonNull Date scannedAt,
            @NonNull List<CEPASPurse> purses,
            @NonNull List<CEPASHistory> histories) {
        return new AutoValue_CEPASCard(tagId, scannedAt, purses, histories);
    }

    @NonNull
    @Override
    public CardType getCardType() {
        return CardType.CEPAS;
    }

    @NonNull
    public abstract List<CEPASPurse> getPurses();

    @NonNull
    public abstract List<CEPASHistory> getHistories();

    @Nullable
    public CEPASPurse getPurse(int purse) {
        return getPurses().get(purse);
    }

    @Nullable
    public CEPASHistory getHistory(int purse) {
        return getHistories().get(purse);
    }

    @NonNull
    @Override
    public FareBotUiTree getAdvancedUi(Context context) {
        FareBotUiTree.Builder cardUiBuilder = FareBotUiTree.builder(context);

        // Add Tag ID Conversion Information
        FareBotUiTree.Item.Builder tagIdBuilder = cardUiBuilder.item().title("Tag ID Information");
        tagIdBuilder.item().title("Tag ID (Hex)").value(getTagId().hex());
        tagIdBuilder.item().title("Tag ID (Raw Bytes)").value(ByteUtils.getHexString(getTagId().bytes(), "ERROR"));
        tagIdBuilder.item().title("Tag ID Length").value(getTagId().bytes().length + " bytes");
        
        // Add detailed conversion process
        FareBotUiTree.Item.Builder conversionBuilder = cardUiBuilder.item().title("Tag ID Conversion Process");
        conversionBuilder.item().title("Algorithm").value("(byte & 0xff) + 0x100 → hex string → substring(1)");
        
        // Show step-by-step conversion for each byte
        byte[] tagIdBytes = getTagId().bytes();
        for (int i = 0; i < tagIdBytes.length; i++) {
            byte value = tagIdBytes[i];
            int unsignedValue = value & 0xff;
            int withOffset = unsignedValue + 0x100;
            String hexString = Integer.toString(withOffset, 16);
            String finalHex = hexString.substring(1);
            
            String stepDescription = String.format("Byte %d: %d (0x%02X) → %d → %d → %s → %s",
                i, value, value, unsignedValue, withOffset, hexString, finalHex);
            conversionBuilder.item().title("Step " + (i + 1)).value(stepDescription);
        }
        
        conversionBuilder.item().title("Final Result").value(getTagId().hex() + " (" + getTagId().hex().length() + " hex characters)");

        FareBotUiTree.Item.Builder pursesUiBuilder = cardUiBuilder.item().title("Purses");
        for (CEPASPurse purse : getPurses()) {
            FareBotUiTree.Item.Builder purseUiBuilder = pursesUiBuilder.item()
                    .title(String.format("Purse ID %s", purse.getId()));
            purseUiBuilder.item().title("CEPAS Version").value(purse.getCepasVersion());
            purseUiBuilder.item().title("Purse Status").value(purse.getPurseStatus());
            purseUiBuilder.item().title("Purse Balance")
                    .value(NumberFormat.getCurrencyInstance(Locale.US).format(purse.getPurseBalance() / 100.0));
            purseUiBuilder.item().title("Purse Creation Date")
                    .value(DateFormat.getDateInstance(DateFormat.LONG).format(purse.getPurseCreationDate() * 1000L));
            purseUiBuilder.item().title("Purse Expiry Date")
                    .value(DateFormat.getDateInstance(DateFormat.LONG).format(purse.getPurseExpiryDate() * 1000L));
            purseUiBuilder.item().title("Autoload Amount").value(purse.getAutoLoadAmount());
            purseUiBuilder.item().title("CAN").value(purse.getCAN());
            purseUiBuilder.item().title("CSN").value(purse.getCSN());

            // Add detailed CAN ID calculation for Purse ID 3
            if (purse.getId() == 3 && purse.getCAN() != null) {
                FareBotUiTree.Item.Builder canCalculationBuilder = purseUiBuilder.item().title("CAN ID Calculation Details");
                
                byte[] canBytes = purse.getCAN().bytes();
                if (canBytes != null && canBytes.length == 8) {
                    // Show CAN bytes in hex format
                    StringBuilder canHex = new StringBuilder();
                    for (byte b : canBytes) {
                        canHex.append(String.format("%02X ", b));
                    }
                    canCalculationBuilder.item().title("CAN Bytes (8-15)").value(canHex.toString().trim());
                    
                    // Convert to 64-bit integer using byteArrayToLong
                    long cardSerial = ByteUtils.byteArrayToLong(canBytes);
                    canCalculationBuilder.item().title("Card Serial (64-bit)").value(String.valueOf(cardSerial));
                    
                    // Show algorithm explanation
                    canCalculationBuilder.item().title("Algorithm").value("(byte & 0xFF) << shift, where shift = (length - 1 - i) * 8");
                    
                    // Show step-by-step calculation
                    long total = 0;
                    for (int i = 0; i < canBytes.length; i++) {
                        int shift = (canBytes.length - 1 - i) * 8;
                        long byteValue = (long) (canBytes[i] & 0x000000FF) << shift;
                        total += byteValue;
                        
                        String stepDescription = String.format("Byte %d: 0x%02X (%d) << %d = %d", 
                            i, canBytes[i] & 0xFF, canBytes[i] & 0xFF, shift, byteValue);
                        canCalculationBuilder.item().title("Step " + (i + 1)).value(stepDescription);
                    }
                    
                    // Show final results
                    canCalculationBuilder.item().title("Total").value(String.valueOf(total));
                    canCalculationBuilder.item().title("Hex").value(String.format("0x%016X", total));
                    canCalculationBuilder.item().title("Binary").value(Long.toBinaryString(total));
                } else {
                    canCalculationBuilder.item().title("Error").value("CAN data is null or not 8 bytes");
                }
            }

            FareBotUiTree.Item.Builder transactionUiBuilder
                    = cardUiBuilder.item().title("Last Transaction Information");
            transactionUiBuilder.item().title("TRP").value(purse.getLastTransactionTRP());
            transactionUiBuilder.item().title("Credit TRP").value(purse.getLastCreditTransactionTRP());
            transactionUiBuilder.item().title("Credit Header").value(purse.getLastCreditTransactionHeader());
            transactionUiBuilder.item().title("Debit Options").value(purse.getLastTransactionDebitOptionsByte());

            FareBotUiTree.Item.Builder otherUiBuilder = cardUiBuilder.item().title("Other Purse Information");
            otherUiBuilder.item().title("Logfile Record Count").value(purse.getLogfileRecordCount());
            otherUiBuilder.item().title("Issuer Data Length").value(purse.getIssuerDataLength());
            otherUiBuilder.item().title("Issuer-specific Data").value(purse.getIssuerSpecificData());
        }

        return cardUiBuilder.build();
    }
}
