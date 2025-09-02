/**
 * CEPAS Protocol Implementation for Web NFC
 * Based on the CEPAS standard for Singapore transit cards
 */

class CEPASProtocol {
    constructor() {
        // CEPAS Application ID (AID)
        this.CEPAS_AID = new Uint8Array([0x40, 0x00]);
        
        // CEPAS commands
        this.COMMANDS = {
            SELECT_FILE: 0xA4,
            READ_PURSE: 0x32
        };
        
        // Purse 3 ID (main transit purse)
        this.PURSE_3_ID = 0x03;
    }

    /**
     * Convert array buffer to hex string
     */
    arrayBufferToHex(buffer) {
        return Array.from(new Uint8Array(buffer))
            .map(b => b.toString(16).padStart(2, '0'))
            .join(' ');
    }

    /**
     * Convert hex string to array buffer
     */
    hexToArrayBuffer(hex) {
        const bytes = hex.replace(/\s/g, '').match(/.{1,2}/g) || [];
        return new Uint8Array(bytes.map(byte => parseInt(byte, 16)));
    }

    /**
     * Convert byte array to 64-bit integer (big-endian)
     * This matches the Java byteArrayToLong implementation
     */
    byteArrayToLong(bytes) {
        let value = 0n;
        for (let i = 0; i < bytes.length; i++) {
            const shift = BigInt((bytes.length - 1 - i) * 8);
            const byteValue = BigInt(bytes[i] & 0xFF) << shift;
            value += byteValue;
        }
        return value;
    }

    /**
     * Parse CEPAS purse data structure
     * Based on the CEPAS standard byte layout
     */
    parsePurseData(purseData, purseId) {
        if (purseData.length < 64) {
            throw new Error(`Invalid purse data length: ${purseData.length} bytes`);
        }

        const data = new Uint8Array(purseData);
        
        // Extract fields based on CEPAS byte structure
        const purse = {
            id: purseId,
            cepasVersion: data[0],
            purseStatus: data[1],
            
            // Balance (bytes 2-4, signed 24-bit)
            purseBalance: this.parseSigned24Bit(data, 2),
            
            // Auto load amount (bytes 5-7, signed 24-bit)
            autoLoadAmount: this.parseSigned24Bit(data, 5),
            
            // CAN ID (bytes 8-15, 8 bytes)
            can: data.slice(8, 16),
            
            // CSN (bytes 16-23, 8 bytes)
            csn: data.slice(16, 24),
            
            // Expiry date (bytes 24-25, days from 1995)
            purseExpiryDate: this.parseDate(data, 24),
            
            // Creation date (bytes 26-27, days from 1995)
            purseCreationDate: this.parseDate(data, 26),
            
            // Last credit transaction TRP (bytes 28-31)
            lastCreditTransactionTRP: this.parseUint32(data, 28),
            
            // Credit header (bytes 32-39)
            lastCreditTransactionHeader: data.slice(32, 40),
            
            // Logfile record count (byte 40)
            logfileRecordCount: data[40],
            
            // Issuer data length (byte 41)
            issuerDataLength: data[41],
            
            // Last transaction TRP (bytes 42-45)
            lastTransactionTRP: this.parseUint32(data, 42),
            
            // Last transaction record (bytes 46-61)
            lastTransactionRecord: data.slice(46, 62),
            
            // Raw data for display
            rawData: data
        };

        return purse;
    }

    /**
     * Parse signed 24-bit value
     */
    parseSigned24Bit(data, offset) {
        let value = (data[offset] << 16) | (data[offset + 1] << 8) | data[offset + 2];
        
        // Sign extend if negative
        if (value & 0x800000) {
            value |= 0xFF000000;
        }
        
        return value;
    }

    /**
     * Parse unsigned 32-bit value
     */
    parseUint32(data, offset) {
        return (data[offset] << 24) | (data[offset + 1] << 16) | 
               (data[offset + 2] << 8) | data[offset + 3];
    }

    /**
     * Parse date (days from 1995-01-01)
     */
    parseDate(data, offset) {
        const days = (data[offset] << 8) | data[offset + 1];
        const epoch1995 = new Date('1995-01-01').getTime();
        return new Date(epoch1995 + (days * 24 * 60 * 60 * 1000));
    }

    /**
     * Format currency (cents to dollars)
     */
    formatCurrency(cents) {
        return `$${(cents / 100).toFixed(2)}`;
    }

    /**
     * Format hex data
     */
    formatHex(data) {
        return Array.from(data)
            .map(b => b.toString(16).padStart(2, '0').toUpperCase())
            .join(' ');
    }

    /**
     * Calculate CAN ID step by step
     */
    calculateCANStepByStep(canBytes) {
        const steps = [];
        let total = 0n;
        
        for (let i = 0; i < canBytes.length; i++) {
            const shift = BigInt((canBytes.length - 1 - i) * 8);
            const byteValue = BigInt(canBytes[i] & 0xFF) << shift;
            total += byteValue;
            
            steps.push({
                byte: i,
                value: canBytes[i] & 0xFF,
                hex: (canBytes[i] & 0xFF).toString(16).padStart(2, '0').toUpperCase(),
                shift: Number(shift),
                result: byteValue.toString()
            });
        }
        
        return {
            steps,
            total: total.toString(),
            hex: '0x' + total.toString(16).toUpperCase(),
            binary: total.toString(2)
        };
    }

    /**
     * Read Purse 3 from NFC tag using Web NFC API
     */
    async readPurse3FromTag(tag) {
        try {
            console.log('Reading CEPAS Purse 3 from tag:', tag);
            
            // Check if tag supports ISO-DEP
            if (!tag.techTypes || !tag.techTypes.includes('IsoDep')) {
                throw new Error('Tag does not support ISO-DEP protocol required for CEPAS cards');
            }

            // Try to read NDEF messages first to get basic tag info
            if (tag.records) {
                console.log('Tag records found:', tag.records.length);
                for (const record of tag.records) {
                    console.log('Record:', record);
                }
            }

            // For real CEPAS cards, we would need to use the Web NFC API's advanced features
            // However, the current Web NFC API has limitations for ISO-DEP communication
            // We'll simulate the reading process and provide guidance for real implementation

            // Simulate reading Purse 3 data (in real implementation, this would be actual card data)
            const simulatedPurseData = this.getSimulatedPurseData();
            const purse = this.parsePurseData(simulatedPurseData, this.PURSE_3_ID);
            const canCalculation = this.calculateCANStepByStep(purse.can);

            return {
                purse,
                canCalculation,
                success: true,
                note: 'This is simulated data. Real implementation requires advanced Web NFC features.'
            };

        } catch (error) {
            console.error('Error reading Purse 3:', error);
            return {
                success: false,
                error: error.message
            };
        }
    }

    /**
     * Alternative method for real CEPAS card reading
     * This would require the Web NFC API to support ISO-DEP communication
     */
    async readRealCEPASCard(tag) {
        try {
            // This is a placeholder for real CEPAS card reading
            // The Web NFC API currently has limitations for ISO-DEP communication
            
            // In a real implementation, you would:
            // 1. Select the CEPAS application using SELECT command
            // 2. Send READ PURSE command for Purse 3
            // 3. Parse the response data
            
            const selectCommand = new Uint8Array([
                this.COMMANDS.SELECT_FILE, 0x00, 0x00, 0x02,
                ...this.CEPAS_AID
            ]);
            
            const readCommand = new Uint8Array([
                this.COMMANDS.READ_PURSE, this.PURSE_3_ID, 0x00, 0x00, 0x01, 0x00
            ]);
            
            // Note: The Web NFC API doesn't currently support sending APDU commands
            // This would require a native app or browser extension
            
            throw new Error('Real CEPAS card reading requires advanced Web NFC features not yet available');
            
        } catch (error) {
            return {
                success: false,
                error: error.message
            };
        }
    }

    // Simulated data for demonstration purposes
    getSimulatedPurseData() {
        // This simulates a real CEPAS Purse 3 data structure
        const data = new Uint8Array(64);
        
        // CEPAS Version
        data[0] = 0x01;
        
        // Purse Status
        data[1] = 0x00;
        
        // Balance: $12.50 (1250 cents)
        data[2] = 0x00;
        data[3] = 0x04;
        data[4] = 0xE2;
        
        // Auto Load Amount: $20.00 (2000 cents)
        data[5] = 0x00;
        data[6] = 0x07;
        data[7] = 0xD0;
        
        // CAN ID: 1C 61 E9 59 00 00 00 00
        data[8] = 0x1C;
        data[9] = 0x61;
        data[10] = 0xE9;
        data[11] = 0x59;
        data[12] = 0x00;
        data[13] = 0x00;
        data[14] = 0x00;
        data[15] = 0x00;
        
        // CSN: 12 34 56 78 9A BC DE F0
        data[16] = 0x12;
        data[17] = 0x34;
        data[18] = 0x56;
        data[19] = 0x78;
        data[20] = 0x9A;
        data[21] = 0xBC;
        data[22] = 0xDE;
        data[23] = 0xF0;
        
        // Expiry Date: 2025-12-31 (days from 1995-01-01)
        const expiryDays = Math.floor((new Date('2025-12-31') - new Date('1995-01-01')) / (24 * 60 * 60 * 1000));
        data[24] = (expiryDays >> 8) & 0xFF;
        data[25] = expiryDays & 0xFF;
        
        // Creation Date: 2020-01-01 (days from 1995-01-01)
        const creationDays = Math.floor((new Date('2020-01-01') - new Date('1995-01-01')) / (24 * 60 * 60 * 1000));
        data[26] = (creationDays >> 8) & 0xFF;
        data[27] = creationDays & 0xFF;
        
        // Logfile Record Count
        data[40] = 0x05;
        
        // Issuer Data Length
        data[41] = 0x00;
        
        return data;
    }

    /**
     * Get implementation notes for real CEPAS card reading
     */
    getImplementationNotes() {
        return {
            currentLimitations: [
                'Web NFC API has limited support for ISO-DEP communication',
                'Cannot send custom APDU commands to CEPAS cards',
                'Only NDEF reading is currently supported',
                'Real CEPAS card reading requires native app or browser extension'
            ],
            alternatives: [
                'Use Android native app with NFC API',
                'Use browser extension with native messaging',
                'Use Progressive Web App with native NFC capabilities',
                'Wait for Web NFC API to support ISO-DEP communication'
            ],
            technicalRequirements: [
                'ISO-DEP protocol support',
                'APDU command transmission',
                'CEPAS application selection',
                'Purse data reading commands'
            ]
        };
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = CEPASProtocol;
}
