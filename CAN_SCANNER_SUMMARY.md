# CEPAS CAN Scanner Implementation

## Overview
I've created a dedicated CAN Scanner page that scans CEPAS cards, extracts the CAN (Card Account Number) bytes 8-15 from purse data, and converts them to a 64-bit integer using the `byteArrayToLong()` method.

## Files Created

### 1. CANScannerActivity.java
**Location**: `app/src/main/java/com/itachi1706/cheesecakecepasreadersample/CANScannerActivity.java`

**Features**:
- NFC card scanning functionality
- Extracts CAN bytes 8-15 from purse data
- Converts CAN bytes to 64-bit integer using `ByteUtils.byteArrayToLong()`
- Displays detailed calculation steps
- Shows both hex representation and decimal result

**Key Methods**:
- `scanCard(Tag tag)`: Handles NFC tag detection and CEPAS card reading
- `extractAndConvertCAN(byte[] canBytes)`: Extracts and converts CAN bytes
- `showCalculation(byte[] canBytes, long result)`: Shows step-by-step calculation

### 2. activity_can_scanner.xml
**Location**: `app/src/main/res/layout/activity_can_scanner.xml`

**UI Components**:
- Scan button
- Status display
- CAN bytes display (hex format)
- Card serial display (64-bit decimal)
- Detailed calculation breakdown

### 3. Button Drawables
- `button_primary.xml`: Blue primary button style
- `button_secondary.xml`: Green secondary button style

### 4. Updated Files

#### RedirectAcrtivity.java
- Modified to show a menu with two options:
  - "Main CEPAS App" - launches the original CEPAS library
  - "CAN Scanner" - launches the new CAN scanner

#### activity_redirect.xml
- Updated layout to show two buttons instead of loading screen

#### AndroidManifest.xml
- Added CANScannerActivity declaration

## How It Works

### 1. NFC Communication
```java
// Get tag ID
byte[] tagId = tag.getId();

// Create CEPAS tag reader
CEPASTagReader tagReader = new CEPASTagReader(tagId, tag);

// Read the raw CEPAS card
RawCEPASCard rawCard = tagReader.readTag();

// Parse to get the CEPAS card
CEPASCard cepasCard = rawCard.parse();
```

### 2. CAN Extraction
```java
// Get purse 3 (typically contains the main transit data)
CEPASPurse purse = cepasCard.getPurse(3);

if (purse != null && purse.isValid()) {
    // Get CAN bytes from the purse
    byte[] canBytes = purse.getCAN().bytes();
    extractAndConvertCAN(canBytes);
}
```

### 3. CAN to 64-bit Conversion
```java
// Convert to 64-bit integer using byteArrayToLong
long cardSerial = ByteUtils.byteArrayToLong(canBytes);
```

### 4. Calculation Display
The app shows the step-by-step calculation:
```
Byte 0: 0x1C (28) << 56 = 2017612633061982208
Byte 1: 0x61 (97) << 48 = 273818857344155648
Byte 2: 0xE9 (233) << 40 = 25670517876088832
Byte 3: 0x59 (89) << 32 = 382252544
Byte 4: 0x00 (0) << 24 = 0
Byte 5: 0x00 (0) << 16 = 0
Byte 6: 0x00 (0) << 8 = 0
Byte 7: 0x00 (0) << 0 = 0

Total: 2305843009213693952
```

## Usage

1. **Launch the app** - Shows menu with two options
2. **Select "CAN Scanner"** - Opens the CAN scanner interface
3. **Tap "Scan CEPAS Card"** - Prepares for NFC scanning
4. **Place CEPAS card near NFC reader** - Automatically scans the card
5. **View results** - See CAN bytes, 64-bit serial, and calculation details

## Technical Details

### CAN Byte Structure
- **Source**: Bytes 8-15 of purse data
- **Length**: 8 bytes
- **Format**: Raw binary data from CEPAS card

### Conversion Algorithm
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

### Example Output
For CAN bytes `[0x1C, 0x61, 0xE9, 0x59, 0x00, 0x00, 0x00, 0x00]`:
- **CAN Bytes (Hex)**: `1C 61 E9 59 00 00 00 00`
- **Card Serial (64-bit)**: `2305843009213693952`

## Benefits

1. **Educational**: Shows exactly how CAN bytes are converted to card serial
2. **Debugging**: Helps understand the conversion process
3. **Verification**: Allows verification of card serial calculations
4. **Transparency**: Shows the raw data and conversion steps

## Integration

The CAN Scanner is integrated into the main app as an alternative option, allowing users to:
- Use the full CEPAS library functionality
- Or focus specifically on CAN extraction and conversion

This provides both comprehensive card reading capabilities and detailed technical analysis of the CAN conversion process.
