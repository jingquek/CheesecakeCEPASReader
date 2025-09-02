# CEPAS Purse 3 CAN ID Reader - Web Application

A web-based application that can read CEPAS Purse 3 CAN ID using Web NFC API. This application demonstrates how to extract the CAN ID from Singapore transit cards (CEPAS) using a generic NFC reader in a web browser.

## Features

- **Web NFC Integration**: Uses the Web NFC API to read CEPAS cards
- **Purse 3 Focus**: Specifically targets Purse 3 (main transit purse)
- **CAN ID Extraction**: Extracts and displays the 8-byte CAN ID from bytes 8-15
- **Step-by-step Calculation**: Shows how the CAN ID is converted to a 64-bit integer
- **Comprehensive Data Display**: Shows all Purse 3 information including balance, dates, etc.
- **Modern UI**: Beautiful, responsive design with real-time feedback

## Requirements

### Browser Support
- **Chrome 89+** on Android
- **Edge 89+** on Android
- **Samsung Internet 15+** on Android
- **Firefox** (experimental support)

### Device Requirements
- **NFC-enabled Android device**
- **HTTPS connection** (required for Web NFC API)
- **CEPAS card** (Singapore transit cards, EZ-Link, etc.)

## Installation & Setup

### 1. Clone or Download
```bash
git clone <repository-url>
cd cepas-web-reader
```

### 2. Serve the Application
Due to Web NFC API requirements, you must serve the application over HTTPS:

#### Option A: Using Python (for development)
```bash
# Python 3
python -m http.server 8000

# Python 2
python -m SimpleHTTPServer 8000
```

#### Option B: Using Node.js
```bash
npx http-server -S -C cert.pem -K key.pem
```

#### Option C: Using Live Server (VS Code)
Install the "Live Server" extension and right-click `index.html` → "Open with Live Server"

### 3. Access the Application
- Open your browser and navigate to `https://localhost:8000`
- Accept any SSL certificate warnings (for local development)

## Usage

### 1. Enable NFC
- Ensure NFC is enabled on your Android device
- Grant NFC permissions when prompted by the browser

### 2. Start Scanning
- Click the "Start NFC Scan" button
- The button will change to "Stop Scanning" and show a pulsing animation

### 3. Scan Your Card
- Place your CEPAS card near the NFC reader on your device
- Hold the card steady for 1-2 seconds
- The application will automatically read the Purse 3 data

### 4. View Results
The application will display:

#### Purse 3 Information
- CEPAS Version
- Purse Status
- Current Balance
- Auto Load Amount
- Card Serial Number (CSN)
- Creation and Expiry Dates
- Logfile Record Count

#### CAN ID Data
- **CAN Bytes (8-15)**: The raw 8-byte CAN ID in hexadecimal format
- **Card Serial (64-bit)**: The calculated 64-bit integer

#### Calculation Details
- Step-by-step breakdown of how each byte contributes to the final value
- Algorithm explanation
- Final result in decimal, hexadecimal, and binary formats

#### Raw Data
- Complete hex dump of the Purse 3 data structure (64 bytes)

## Technical Implementation

### CEPAS Protocol
The application implements the CEPAS (Contactless ePurse Application Standard) protocol:

#### Byte Structure
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
40            | Logfile Record Count| 1    | Number of records
41            | Issuer Data Length  | 1    | Length of issuer data
42-45         | Last Transaction TRP| 4    | Last transaction reference
46-61         | Last Transaction    | 16   | Last transaction record
62+           | Issuer Specific Data| var  | Variable length data
```

#### CAN ID Calculation
The CAN ID is converted to a 64-bit integer using big-endian conversion:

```javascript
function byteArrayToLong(bytes) {
    let value = 0n;
    for (let i = 0; i < bytes.length; i++) {
        const shift = BigInt((bytes.length - 1 - i) * 8);
        const byteValue = BigInt(bytes[i] & 0xFF) << shift;
        value += byteValue;
    }
    return value;
}
```

### Web NFC API
The application uses the Web NFC API to communicate with NFC tags:

```javascript
const ndef = new NDEFReader();
ndef.addEventListener('reading', async (event) => {
    const tag = event.target;
    // Process the NFC tag
});
await ndef.scan();
```

## File Structure

```
cepas-web-reader/
├── index.html          # Main HTML file
├── styles.css          # CSS styles
├── cepas-protocol.js   # CEPAS protocol implementation
├── app.js             # Main application logic
└── README.md          # This file
```

## Troubleshooting

### Common Issues

#### 1. "Web NFC API not supported"
- **Solution**: Use Chrome 89+ or Edge 89+ on Android
- **Alternative**: Check browser compatibility at [Web NFC API support](https://caniuse.com/web-nfc)

#### 2. "NFC permission denied"
- **Solution**: Enable NFC in your device settings
- **Alternative**: Grant NFC permissions in browser settings

#### 3. "HTTPS required"
- **Solution**: Access the application via HTTPS
- **Alternative**: Use localhost for development (HTTPS not required)

#### 4. "Tag does not support ISO-DEP"
- **Solution**: Ensure you're using a CEPAS card (not all NFC cards support ISO-DEP)
- **Alternative**: Try a different CEPAS card

#### 5. "Failed to read Purse 3"
- **Solution**: Hold the card steady for longer
- **Alternative**: Try repositioning the card on the NFC reader

### Debug Mode
Open the browser's Developer Tools (F12) to see detailed error messages and debug information.

## Security Considerations

- **HTTPS Required**: Web NFC API only works over secure connections
- **User Permission**: NFC access requires explicit user permission
- **Data Privacy**: The application only reads public data from the card
- **No Data Storage**: No card data is stored or transmitted

## Limitations

- **Browser Support**: Limited to specific browsers on Android
- **Device Requirements**: Requires NFC-enabled Android device
- **Card Type**: Only works with CEPAS-compliant cards
- **Purse 3 Focus**: Only reads Purse 3 data (main transit purse)

## Future Enhancements

- Support for other CEPAS purses (0-15)
- Transaction history reading
- Offline mode with cached data
- Export functionality for data analysis
- Support for other transit card formats

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly on NFC-enabled devices
5. Submit a pull request

## License

This project is open source and available under the MIT License.

## Acknowledgments

- Based on the CEPAS (Contactless ePurse Application Standard)
- Inspired by the original Android CEPAS reader implementation
- Uses Web NFC API for modern web-based NFC communication

## Support

For issues and questions:
1. Check the troubleshooting section above
2. Review browser compatibility requirements
3. Ensure your device and card meet the requirements
4. Open an issue on the project repository

---

**Note**: This application is for educational and demonstration purposes. Always respect privacy and security when working with transit card data.
