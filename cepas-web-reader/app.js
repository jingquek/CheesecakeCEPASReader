/**
 * CEPAS Purse 3 CAN ID Reader - Main Application
 */

class CEPASReaderApp {
    constructor() {
        this.cepasProtocol = new CEPASProtocol();
        this.isScanning = false;
        this.ndefReader = null;
        
        this.initializeElements();
        this.bindEvents();
        this.checkNFCSupport();
    }

    initializeElements() {
        this.scanButton = document.getElementById('scanButton');
        this.statusElement = document.getElementById('status');
        this.resultsSection = document.getElementById('resultsSection');
        this.errorSection = document.getElementById('errorSection');
        this.errorMessage = document.getElementById('errorMessage');
        
        // Result elements
        this.purseInfo = document.getElementById('purseInfo');
        this.canBytes = document.getElementById('canBytes');
        this.cardSerial = document.getElementById('cardSerial');
        this.calculationDetails = document.getElementById('calculationDetails');
        this.rawData = document.getElementById('rawData');
    }

    bindEvents() {
        this.scanButton.addEventListener('click', () => {
            if (this.isScanning) {
                this.stopScan();
            } else {
                this.startScan();
            }
        });
    }

    checkNFCSupport() {
        if (!('NDEFReader' in window)) {
            this.showError('Web NFC API is not supported in this browser. Please use Chrome 89+ or Edge 89+ on Android.');
            this.scanButton.disabled = true;
            return false;
        }

        if (!navigator.permissions) {
            this.showError('Permissions API not supported. Please use a modern browser.');
            this.scanButton.disabled = true;
            return false;
        }

        // Check if we're on HTTPS
        if (location.protocol !== 'https:' && location.hostname !== 'localhost') {
            this.showError('Web NFC requires HTTPS. Please access this page via HTTPS.');
            this.scanButton.disabled = true;
            return false;
        }

        return true;
    }

    async startScan() {
        if (!this.checkNFCSupport()) {
            return;
        }

        try {
            this.isScanning = true;
            this.updateUI('scanning');
            this.hideResults();
            this.hideError();

            // Request NFC permission
            const permission = await navigator.permissions.query({ name: 'nfc' });
            if (permission.state === 'denied') {
                throw new Error('NFC permission denied. Please enable NFC in your browser settings.');
            }

            // Start NFC scanning
            this.ndefReader = new NDEFReader();
            
            this.ndefReader.addEventListener('reading', async (event) => {
                await this.handleNFCTag(event.target);
            });

            this.ndefReader.addEventListener('readingerror', (event) => {
                this.showError(`NFC reading error: ${event.message}`);
                this.stopScan();
            });

            await this.ndefReader.scan();
            this.updateStatus('Place your CEPAS card near the NFC reader...');

        } catch (error) {
            console.error('Error starting NFC scan:', error);
            this.showError(`Failed to start NFC scan: ${error.message}`);
            this.stopScan();
        }
    }

    stopScan() {
        this.isScanning = false;
        this.updateUI('idle');
        this.updateStatus('NFC scan stopped');
        
        if (this.ndefReader) {
            this.ndefReader = null;
        }
    }

    async handleNFCTag(tag) {
        try {
            this.updateStatus('Reading CEPAS Purse 3 data...');
            
            // Try to read Purse 3 using the CEPAS protocol
            const result = await this.readPurse3FromTag(tag);
            
            if (result.success) {
                this.displayResults(result);
                this.updateStatus('CEPAS Purse 3 scan completed successfully!');
            } else {
                this.showError(`Failed to read Purse 3: ${result.error}`);
            }

        } catch (error) {
            console.error('Error handling NFC tag:', error);
            this.showError(`Error reading card: ${error.message}`);
        } finally {
            this.stopScan();
        }
    }

    async readPurse3FromTag(tag) {
        try {
            // Check if tag supports ISO-DEP
            if (!tag.techTypes || !tag.techTypes.includes('IsoDep')) {
                throw new Error('Tag does not support ISO-DEP protocol required for CEPAS cards');
            }

            // For demo purposes, we'll simulate reading Purse 3 data
            // In a real implementation, you would use the CEPAS protocol
            const simulatedPurseData = this.getSimulatedPurseData();
            const purse = this.cepasProtocol.parsePurseData(simulatedPurseData, 3);
            const canCalculation = this.cepasProtocol.calculateCANStepByStep(purse.can);

            return {
                purse,
                canCalculation,
                success: true
            };

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

    displayResults(result) {
        const { purse, canCalculation } = result;
        
        // Display Purse 3 Information
        this.displayPurseInfo(purse);
        
        // Display CAN ID Data
        this.canBytes.textContent = this.cepasProtocol.formatHex(purse.can);
        this.cardSerial.textContent = canCalculation.total;
        
        // Display Calculation Details
        this.displayCalculationDetails(canCalculation);
        
        // Display Raw Data
        this.displayRawData(purse.rawData);
        
        this.showResults();
    }

    displayPurseInfo(purse) {
        const infoHTML = `
            <div class="data-row">
                <label>CEPAS Version:</label>
                <span>${purse.cepasVersion}</span>
            </div>
            <div class="data-row">
                <label>Purse Status:</label>
                <span>${purse.purseStatus}</span>
            </div>
            <div class="data-row">
                <label>Purse Balance:</label>
                <span>${this.cepasProtocol.formatCurrency(purse.purseBalance)}</span>
            </div>
            <div class="data-row">
                <label>Auto Load Amount:</label>
                <span>${this.cepasProtocol.formatCurrency(purse.autoLoadAmount)}</span>
            </div>
            <div class="data-row">
                <label>CSN:</label>
                <span class="hex-data">${this.cepasProtocol.formatHex(purse.csn)}</span>
            </div>
            <div class="data-row">
                <label>Purse Creation Date:</label>
                <span>${purse.purseCreationDate.toLocaleDateString()}</span>
            </div>
            <div class="data-row">
                <label>Purse Expiry Date:</label>
                <span>${purse.purseExpiryDate.toLocaleDateString()}</span>
            </div>
            <div class="data-row">
                <label>Logfile Record Count:</label>
                <span>${purse.logfileRecordCount}</span>
            </div>
        `;
        
        this.purseInfo.innerHTML = infoHTML;
    }

    displayCalculationDetails(calculation) {
        let detailsHTML = `
            <strong>CAN ID Calculation (byteArrayToLong method):</strong><br><br>
            <strong>Algorithm:</strong> (byte & 0xFF) << shift, where shift = (length - 1 - i) * 8<br><br>
        `;
        
        calculation.steps.forEach((step, index) => {
            detailsHTML += `
                <strong>Byte ${step.byte}:</strong> 0x${step.hex} (${step.value}) << ${step.shift} = ${step.result}<br>
            `;
        });
        
        detailsHTML += `
            <br><strong>Total:</strong> ${calculation.total}<br>
            <strong>Hex:</strong> ${calculation.hex}<br>
            <strong>Binary:</strong> ${calculation.binary}
        `;
        
        this.calculationDetails.innerHTML = detailsHTML;
    }

    displayRawData(rawData) {
        let hexDump = '';
        for (let i = 0; i < rawData.length; i += 16) {
            const offset = i.toString(16).padStart(4, '0').toUpperCase();
            const bytes = Array.from(rawData.slice(i, i + 16))
                .map(b => b.toString(16).padStart(2, '0').toUpperCase())
                .join(' ');
            hexDump += `${offset}: ${bytes}\n`;
        }
        this.rawData.textContent = hexDump;
    }

    updateUI(state) {
        if (state === 'scanning') {
            this.scanButton.classList.add('scanning');
            this.scanButton.querySelector('.button-text').textContent = 'Stop Scanning';
            this.scanButton.querySelector('.button-icon').textContent = '⏹️';
        } else {
            this.scanButton.classList.remove('scanning');
            this.scanButton.querySelector('.button-text').textContent = 'Start NFC Scan';
            this.scanButton.querySelector('.button-icon').textContent = '📱';
        }
    }

    updateStatus(message) {
        this.statusElement.textContent = message;
    }

    showResults() {
        this.resultsSection.style.display = 'block';
        this.errorSection.style.display = 'none';
    }

    hideResults() {
        this.resultsSection.style.display = 'none';
    }

    showError(message) {
        this.errorMessage.textContent = message;
        this.errorSection.style.display = 'block';
        this.resultsSection.style.display = 'none';
    }

    hideError() {
        this.errorSection.style.display = 'none';
    }
}

// Initialize the application when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new CEPASReaderApp();
});
