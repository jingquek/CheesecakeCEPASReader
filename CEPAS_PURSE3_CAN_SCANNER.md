# CEPAS Purse 3 CAN Scanner - Enhanced Implementation

## Overview
This app has been modified to specifically scan for **CEPAS Purse 3 data only** and provides detailed CAN ID calculation information. The scanner now focuses exclusively on Purse 3, which typically contains the main transit data for CEPAS cards.

## Key Modifications

### 1. Exclusive Purse 3 Scanning
- **Before**: Scanned all purses (0-15) if Purse 3 was not available
- **After**: Only scans Purse 3, shows clear error if Purse 3 is not found
- **Benefit**: Focused functionality, clearer user experience

### 2. Enhanced CAN ID Calculation Display
The app now shows comprehensive calculation details:

#### Algorithm Explanation
```
CAN ID Calculation (byteArrayToLong method):

Algorithm: (byte & 0xFF) << shift, where shift = (length - 1 - i) * 8
```

#### Step-by-Step Calculation
For each byte in the CAN (bytes 8-15):
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
Hex: 0x1C61E95900000000
Binary: 111000110000111110100101011001000000000000000000000000000000000
```

### 3. Purse Information Display
Added comprehensive Purse 3 information:
- CEPAS Version
- Purse Status
- Purse Balance (formatted as currency)
- Auto Load Amount (formatted as currency)
- CSN (Card Serial Number)
- Purse Creation Date
- Purse Expiry Date
- Logfile Record Count

### 4. Improved User Interface
- Updated titles to clearly indicate "CEPAS Purse 3" focus
- Enhanced error messages for Purse 3-specific issues
- Better visual organization of information
- Monospace font for technical data display

## Technical Implementation

### CAN Extraction Process
```java
// Only scan Purse 3
CEPASPurse purse = cepasCard.getPurse(3);

if (purse != null && purse.isValid()) {
    // Get CAN bytes from Purse 3
    byte[] canBytes = purse.getCAN().bytes();
    extractAndConvertCAN(canBytes);
} else {
    // Show Purse 3 specific error
    tvStatus.setText("Error: CEPAS Purse 3 not found or invalid. This app only scans for Purse 3 data.");
}
```

### CAN to 64-bit Conversion
```java
// Using ByteUtils.byteArrayToLong method
long cardSerial = ByteUtils.byteArrayToLong(canBytes);

// The method implements:
// for (int i = 0; i < length; i++) {
//     int shift = (length - 1 - i) * 8;
//     value += (long) (b[i + offset] & 0x000000FF) << shift;
// }
```

### Purse Information Display
```java
private void displayPurseInfo(CEPASPurse purse) {
    // Shows comprehensive Purse 3 data including:
    // - Balance and amounts in currency format
    // - Dates in readable format
    // - CSN in hex format
    // - All relevant purse metadata
}
```

## File Modifications

### 1. CANScannerActivity.java
- **Modified**: `scanCard()` method to only scan Purse 3
- **Added**: `displayPurseInfo()` method for comprehensive purse data
- **Enhanced**: `showCalculation()` method with detailed algorithm explanation
- **Updated**: Error handling for Purse 3-specific scenarios

### 2. activity_can_scanner.xml
- **Updated**: Title to "CEPAS Purse 3 CAN Scanner"
- **Added**: New TextView for purse information display
- **Enhanced**: Button text to "Scan CEPAS Purse 3"
- **Improved**: Section titles for clarity

### 3. activity_redirect.xml
- **Updated**: Button text to "CEPAS Purse 3 CAN Scanner"

## Usage Instructions

1. **Launch the app** - Shows menu with two options
2. **Select "CEPAS Purse 3 CAN Scanner"** - Opens the specialized scanner
3. **Tap "Scan CEPAS Purse 3"** - Prepares for NFC scanning
4. **Place CEPAS card near NFC reader** - Automatically scans Purse 3
5. **View comprehensive results**:
   - Purse 3 information (balance, dates, etc.)
   - CAN bytes in hex format
   - 64-bit card serial number
   - Detailed calculation breakdown

## Error Handling

### Purse 3 Not Found
- Clear error message: "CEPAS Purse 3 not found or invalid. This app only scans for Purse 3 data."
- No fallback to other purses
- All display fields show "Not available"

### CAN Data Issues
- Validates CAN bytes are exactly 8 bytes
- Shows specific error for null CAN data
- Graceful handling of parsing errors

## Benefits of Purse 3 Focus

1. **Specificity**: Only scans the most relevant purse for transit data
2. **Clarity**: Clear indication of what the app does
3. **Performance**: Faster scanning (no need to check all purses)
4. **Reliability**: Consistent behavior across different card types
5. **Educational**: Shows exactly how CAN ID is calculated from Purse 3 data

## Technical Details

### CAN Byte Structure in Purse 3
- **Position**: Bytes 8-15 of purse data
- **Length**: 8 bytes (64 bits)
- **Format**: Raw binary data from CEPAS card
- **Purpose**: Unique card identifier for transit systems

### Conversion Algorithm
The `byteArrayToLong` method implements big-endian conversion:
- Each byte is treated as unsigned (0-255)
- Bytes are shifted left by their position
- Most significant byte (byte 0) gets the highest shift (56 bits)
- Least significant byte (byte 7) gets no shift (0 bits)

This produces a unique 64-bit integer that serves as the card's serial number for transit system identification.


