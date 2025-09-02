# CEPAS Byte Structure: How the App Checks for Purse 3 and CAN ID

## Overview
This document explains exactly how the CEPAS app identifies and extracts Purse 3 data and the CAN ID from a standard NFC scan, including the specific byte positions and structure.

## CEPAS Card Structure

### 1. Card Organization
A CEPAS card contains **16 purses** (numbered 0-15), each with its own data structure. The app specifically targets **Purse 3** because it typically contains the main transit data.

### 2. How the App Identifies Purse 3

```java
// From CEPASCard.java - line 179
CEPASPurse purse = cepasCard.getPurse(3);

// The app checks:
if (purse != null && purse.isValid()) {
    // Purse 3 exists and is valid
}
```

**Purse 3 Identification Process:**
1. **NFC Scan**: The app scans all 16 purses (0-15) from the card
2. **Purse Selection**: Specifically looks for purse with ID = 3
3. **Validation**: Checks if Purse 3 exists and is valid
4. **Data Extraction**: If valid, extracts the purse data

## CEPAS Purse Data Structure

### Standard Purse Byte Layout
Each purse contains a fixed structure of bytes. Here's the complete byte layout:

```
Byte Position | Field Name           | Size | Description
--------------|---------------------|------|-------------
0             | CEPAS Version       | 1    | CEPAS protocol version
1             | Purse Status        | 1    | Current purse status
2-4           | Purse Balance       | 3    | Current balance (signed)
5-7           | Auto Load Amount    | 3    | Auto-reload amount (signed)
8-15          | CAN (Card Account)  | 8    | **CAN ID - 8 bytes**
16-23         | CSN (Card Serial)   | 8    | Card serial number
24-25         | Purse Expiry Date   | 2    | Expiry date (days from 1995)
26-27         | Purse Creation Date | 2    | Creation date (days from 1995)
28-31         | Last Credit TRP     | 4    | Last credit transaction
32-39         | Credit Header       | 8    | Credit transaction header
40            | Logfile Record Count| 1    | Number of transaction records
41            | Issuer Data Length  | 1    | Length of issuer data
42-45         | Last Transaction TRP| 4    | Last transaction reference
46-61         | Last Transaction    | 16   | Last transaction record
62+           | Issuer Specific Data| var  | Variable length issuer data
```

### CAN ID Location: Bytes 8-15

**The CAN ID is specifically located at bytes 8-15 of each purse data structure.**

```java
// From CEPASPurse.java - lines 95-97
byte[] can = new byte[8];
System.arraycopy(purseData, 8, can, 0, can.length);
```

**CAN ID Extraction Process:**
1. **Position**: Bytes 8-15 (8 bytes total)
2. **Format**: Raw binary data
3. **Purpose**: Unique card identifier for transit systems
4. **Conversion**: Converted to 64-bit integer using `byteArrayToLong()`

## NFC Scan Process

### 1. Initial NFC Communication
```java
// From CEPASTagReader.java
CEPASTagReader tagReader = new CEPASTagReader(tagId, tag);
RawCEPASCard rawCard = tagReader.readTag();
```

### 2. Purse Scanning
```java
// From CEPASTagReader.java - lines 25-30
for (int purseId = 0; purseId < purses.length; purseId++) {
    purses[purseId] = protocol.getPurse(purseId);
}
```

**The app scans ALL 16 purses (0-15) but specifically uses Purse 3.**

### 3. CEPAS Protocol Commands
```java
// From CEPASProtocol.java - line 58
byte[] purseBuff = sendRequest((byte) 0x32, (byte) (purseId), (byte) 0, (byte) 0, new byte[]{(byte) 0});
```

**NFC Commands:**
- **Select File**: `00 A4 00 00 02 40 00` (Select CEPAS application)
- **Read Purse**: `32 [purse_id] 00 00 01 00` (Read specific purse data)

## CAN ID Calculation Process

### 1. Raw CAN Bytes Extraction
```java
// From CEPASPurse.java
byte[] can = new byte[8];
System.arraycopy(purseData, 8, can, 0, can.length);
```

### 2. CAN to 64-bit Conversion
```java
// From ByteUtils.java
long cardSerial = ByteUtils.byteArrayToLong(canBytes);
```

**Algorithm:**
```java
public static long byteArrayToLong(byte[] b, int offset, int length) {
    long value = 0;
    for (int i = 0; i < length; i++) {
        int shift = (length - 1 - i) * 8;
        value += (long) (b[i + offset] & 0x000000FF) << shift;
    }
    return value;
}
```

## Example: Complete Data Flow

### Sample Purse 3 Data (64 bytes)
```
Position | Hex Value | Description
---------|-----------|-------------
0        | 01        | CEPAS Version
1        | 00        | Purse Status
2-4      | 00 00 50  | Balance ($0.80)
5-7      | 00 00 00  | Auto Load Amount
8-15     | 1C 61 E9 59 00 00 00 00 | **CAN ID**
16-23    | 12 34 56 78 9A BC DE F0 | CSN
24-25    | 00 00     | Expiry Date
26-27    | 00 00     | Creation Date
...      | ...       | (remaining data)
```

### CAN ID Processing
```
Raw CAN Bytes: 1C 61 E9 59 00 00 00 00

Step-by-step calculation:
Byte 0: 0x1C (28) << 56 = 2017612633061982208
Byte 1: 0x61 (97) << 48 = 273818857344155648
Byte 2: 0xE9 (233) << 40 = 25670517876088832
Byte 3: 0x59 (89) << 32 = 382252544
Byte 4: 0x00 (0) << 24 = 0
Byte 5: 0x00 (0) << 16 = 0
Byte 6: 0x00 (0) << 8 = 0
Byte 7: 0x00 (0) << 0 = 0

Total: 2045172274264276992
```

## Why Purse 3?

**Purse 3 is specifically targeted because:**
1. **Transit Data**: Contains the main transit/transportation data
2. **Standard Location**: Most CEPAS cards use Purse 3 for transit
3. **Consistency**: Provides reliable data across different card types
4. **CAN ID**: Typically contains the most relevant CAN ID for transit systems

## Error Handling

### Purse 3 Not Found
```java
if (purse == null || !purse.isValid()) {
    // Show error: "CEPAS Purse 3 not found or invalid"
}
```

### CAN Data Issues
```java
if (canBytes == null || canBytes.length != 8) {
    // Show error: "CAN data is null or not 8 bytes"
}
```

## Technical Summary

1. **NFC Scan**: Reads all 16 purses from the CEPAS card
2. **Purse Selection**: Specifically targets Purse 3 (ID = 3)
3. **CAN Extraction**: Extracts bytes 8-15 from Purse 3 data
4. **Conversion**: Converts 8-byte CAN to 64-bit integer
5. **Display**: Shows detailed calculation process

The app's focus on Purse 3 and bytes 8-15 for the CAN ID is based on the CEPAS standard specification, where Purse 3 contains the primary transit data and the CAN ID is always located at bytes 8-15 of each purse structure.

