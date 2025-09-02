# CEPAS Card Reader Core

This folder contains the essential files that handle the process of reading CEPAS (Contactless ePurse Application Standard) cards. These files work together to provide a complete solution for NFC-based CEPAS card reading and CAN ID extraction.

## Overview

CEPAS cards are smart cards used for contactless payment and transit systems, particularly in Singapore. This implementation provides the core functionality to read CEPAS cards via NFC and extract specific data including the CAN (Card Account Number) ID.

## Files Overview

### 1. **CEPASTagReader.java** - Main Reader Class
**Primary Role**: Orchestrates the entire CEPAS card reading process

**Key Responsibilities**:
- Extends `TagReader<IsoDep, RawCEPASCard, CardKeys>`
- Creates a `CEPASProtocol` instance to handle low-level communication
- Reads all 16 purses (0-15) from the CEPAS card
- Reads transaction history for each valid purse
- Returns a `RawCEPASCard` object containing all the data

**Main Method**: `readTag()` - Reads the entire card and returns structured data

### 2. **CEPASProtocol.java** - Low-Level Communication
**Primary Role**: Handles the low-level NFC communication with CEPAS cards

**Key Responsibilities**:
- Sends CEPAS application selection command (`00 A4 00 00 02 40 00`)
- Sends READ PURSE commands (`32 [purse_id] 00 00 01 00`)
- Handles APDU command wrapping and response parsing
- Manages error handling and status codes
- Implements the CEPAS protocol specification

**Key Methods**:
- `getPurse(int purseId)` - Reads a specific purse from the card
- `sendRequest()` - Sends APDU commands to the card
- `sendSelectFile()` - Selects the CEPAS application

### 3. **CANScannerActivity.java** - User Interface & Orchestration
**Primary Role**: Provides the Android user interface and orchestrates the scanning process

**Key Responsibilities**:
- Handles NFC intent detection when a card is placed near the reader
- Creates the `CEPASTagReader` instance
- Calls `tagReader.readTag()` to read the card
- Parses raw card data into `CEPASCard` objects
- Specifically extracts Purse 3 data and CAN ID information
- Displays results to the user with detailed calculations

**Key Methods**:
- `scanCard(Tag tag)` - Main scanning orchestration
- `extractAndConvertCAN(byte[] canBytes)` - CAN ID processing
- `displayPurseInfo(CEPASPurse purse)` - Information display

## How the CEPAS Card Reading Process Works

### 1. NFC Detection
- `CANScannerActivity` detects when a CEPAS card is placed near the NFC reader
- Android system sends an NFC intent with tag information

### 2. Reader Creation
- Creates a `CEPASTagReader` with the tag ID and NFC tag object
- Initializes the ISO-DEP technology for communication

### 3. Protocol Communication
`CEPASTagReader` uses `CEPASProtocol` to:
- **Select CEPAS Application**: Send `00 A4 00 00 02 40 00` to select the CEPAS app
- **Read All Purses**: Iterate through all 16 purses (0-15) using READ PURSE commands
- **Read History**: For valid purses, read transaction history records

### 4. Data Parsing
- Raw binary data from the card is parsed into structured `CEPASCard` objects
- Each purse contains specific data fields (balance, CAN ID, CSN, etc.)

### 5. Specific Extraction
- The app focuses on **Purse 3** (typically contains main transit data)
- Extracts the **CAN ID** from bytes 8-15 of the purse data
- Converts the 8-byte CAN ID to a 64-bit integer

### 6. Display Results
- Shows purse information (balance, status, dates, etc.)
- Displays CAN bytes in hexadecimal format
- Shows the calculated 64-bit card serial number
- Provides step-by-step calculation details

## CEPAS Card Structure

### Purse Data Layout (64 bytes)
```
Byte Position | Field Name           | Size | Description
--------------|---------------------|------|-------------
0             | CEPAS Version       | 1    | Protocol version
1             | Purse Status        | 1    | Current status
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

## CAN ID Processing

### Extraction Process
1. **Location**: Bytes 8-15 of each purse data structure
2. **Format**: Raw 8-byte binary data
3. **Purpose**: Unique card identifier for transit systems

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

### Example Calculation
For CAN bytes `[0x1C, 0x61, 0xE9, 0x59, 0x00, 0x00, 0x00, 0x00]`:
```
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

## NFC Commands Used

### CEPAS Application Selection
```
Command: 00 A4 00 00 02 40 00
- CLA: 00 (ISO 7816-4)
- INS: A4 (SELECT)
- P1: 00 (Select by name)
- P2: 00 (First or only occurrence)
- Lc: 02 (Length of AID)
- Data: 40 00 (CEPAS Application ID)
```

### Read Purse Command
```
Command: 32 [purse_id] 00 00 01 00
- CLA: 90 (CEPAS specific)
- INS: 32 (READ PURSE)
- P1: [purse_id] (0-15)
- P2: 00 (Read purse data)
- Lc: 01 (Length of data)
- Data: 00 (Read from beginning)
```

## Error Handling

### Common Error Scenarios
1. **Purse 3 Not Found**: Card doesn't have a valid Purse 3
2. **CAN Data Issues**: CAN bytes are null or not 8 bytes
3. **NFC Communication Errors**: Card not properly detected
4. **Permission Denied**: Card requires authentication

### Error Response Codes
- `0x90 0x00`: Operation successful
- `0x90 0x9D`: Permission denied
- `0x6B`: Invalid file reference
- `0x67`: Invalid file size

## Dependencies

### Android NFC Requirements
- `android.nfc.Tag` - NFC tag interface
- `android.nfc.tech.IsoDep` - ISO-DEP technology
- NFC permissions in AndroidManifest.xml

### Internal Dependencies
- `TagReader` base class
- `RawCEPASCard`, `RawCEPASPurse`, `RawCEPASHistory` data classes
- `CEPASException` for error handling
- `ByteUtils` for data conversion

## Usage Example

```java
// Create tag reader
CEPASTagReader tagReader = new CEPASTagReader(tagId, tag);

// Read the raw CEPAS card
RawCEPASCard rawCard = tagReader.readTag();

// Parse to get the CEPAS card
CEPASCard cepasCard = rawCard.parse();

// Get Purse 3 (main transit data)
CEPASPurse purse = cepasCard.getPurse(3);

if (purse != null && purse.isValid()) {
    // Extract CAN ID
    byte[] canBytes = purse.getCAN().bytes();
    long cardSerial = ByteUtils.byteArrayToLong(canBytes);
    
    // Use the CAN ID for transit system identification
    System.out.println("Card Serial: " + cardSerial);
}
```

## Technical Notes

### Why Purse 3?
- **Transit Data**: Contains the main transit/transportation data
- **Standard Location**: Most CEPAS cards use Purse 3 for transit
- **Consistency**: Provides reliable data across different card types
- **CAN ID**: Typically contains the most relevant CAN ID for transit systems

### Performance Considerations
- Reading all 16 purses takes time but ensures complete data
- Transaction history reading is optional and can be skipped
- Error handling prevents crashes on invalid cards

### Security Notes
- This implementation only reads public data from CEPAS cards
- No authentication or encryption keys are required
- All data read is publicly accessible via NFC

## License

This code is part of the FareBot project and is licensed under the GNU General Public License v3.0.

## Contributing

When modifying these files:
1. Maintain backward compatibility with existing CEPAS cards
2. Follow the established error handling patterns
3. Test with real CEPAS cards to ensure reliability
4. Document any protocol changes or additions
