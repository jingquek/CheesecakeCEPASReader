# Android 14 NFC Troubleshooting Guide

## Overview
This guide addresses common NFC scanning issues on Android 14 devices when using the CEPAS Purse 3 CAN Scanner app.

## Common Issues and Solutions

### 1. App Not Responding to Card Scans

**Symptoms:**
- Pressing "Scan CEPAS Purse 3" button shows "Ready to scan" but nothing happens when card is placed
- No error messages appear
- App appears to be waiting indefinitely

**Solutions:**
1. **Check NFC Permissions:**
   - Go to Settings > Apps > CEPAS Reader > Permissions
   - Ensure NFC permission is granted
   - If not granted, manually enable it

2. **Verify NFC is Enabled:**
   - Go to Settings > Connected devices > Connection preferences > NFC
   - Ensure NFC is turned ON
   - Ensure "Use NFC" is enabled

3. **Check App Permissions:**
   - Go to Settings > Apps > CEPAS Reader > Permissions
   - Ensure all permissions are granted
   - If any are denied, manually grant them

### 2. Card Detected But No Data Retrieved

**Symptoms:**
- App shows "Tag discovered" in logs but fails to read card data
- Error messages like "Permission denied" or "Invalid response"
- Card is detected but scan fails

**Solutions:**
1. **Card Placement:**
   - Place card slowly and steadily on the NFC reader area
   - Hold card in place for 2-3 seconds
   - Avoid rapid movement or lifting

2. **Card Type Verification:**
   - Ensure you're using a valid CEPAS card
   - CEPAS cards are typically used in Singapore transit systems
   - Other card types (credit cards, access cards) won't work

3. **Card Condition:**
   - Check if card is physically damaged
   - Ensure card is not bent or cracked
   - Clean card surface if dirty

### 3. "Permission Denied" Errors

**Symptoms:**
- Error message: "Permission denied" or "CEPASException: Permission denied"
- Card is detected but communication fails

**Solutions:**
1. **Card Authentication:**
   - Some CEPAS cards require authentication
   - Try scanning the card multiple times
   - Ensure card is properly activated

2. **App Permissions:**
   - Go to Settings > Apps > CEPAS Reader > Permissions
   - Grant all requested permissions
   - Restart the app after granting permissions

### 4. "Invalid Response" or "File Not Found" Errors

**Symptoms:**
- Error messages about invalid responses or file references
- Card communication starts but fails during data retrieval

**Solutions:**
1. **Card Compatibility:**
   - Verify the card is a genuine CEPAS card
   - Some older cards may not support all features
   - Contact card issuer for compatibility information

2. **NFC Reader Compatibility:**
   - Ensure device NFC reader supports ISO-DEP protocol
   - Some budget devices may have limited NFC capabilities

### 5. App Crashes or Freezes

**Symptoms:**
- App crashes when scanning
- App becomes unresponsive
- Force close dialogs appear

**Solutions:**
1. **Clear App Data:**
   - Go to Settings > Apps > CEPAS Reader > Storage
   - Clear app data and cache
   - Restart the app

2. **Update App:**
   - Check for app updates in Google Play Store
   - Install latest version if available

3. **Device Restart:**
   - Restart your Android device
   - This can resolve NFC driver issues

## Android 14 Specific Issues

### 1. Foreground Dispatch Problems

**Issue:** Android 14 has stricter requirements for NFC foreground dispatch

**Solution:** The updated app now properly handles foreground dispatch with:
- Proper PendingIntent flags (`FLAG_MUTABLE`)
- Better lifecycle management
- Enhanced error handling

### 2. Permission Changes

**Issue:** Android 14 introduced new permission requirements

**Solution:** The app now:
- Requests all necessary permissions explicitly
- Handles permission denials gracefully
- Provides clear guidance on required permissions

### 3. Intent Filter Priority

**Issue:** Android 14 may handle NFC intents differently

**Solution:** The updated manifest now includes:
- Priority-based intent filters
- Better technology support
- Enhanced NFC compatibility

## Debugging Steps

### 1. Enable Developer Options
1. Go to Settings > About phone
2. Tap "Build number" 7 times
3. Go back to Settings > System > Developer options
4. Enable "USB debugging" and "Show all ANRs"

### 2. Check Logcat
1. Connect device to computer
2. Use Android Studio or ADB to view logs
3. Filter by tag "CANScannerActivity"
4. Look for NFC-related errors

### 3. Test NFC Functionality
1. Use another NFC app to test basic NFC
2. Try scanning other NFC tags
3. Verify device NFC hardware is working

## Device-Specific Solutions

### Samsung Devices
- Check "Smart Things" app for NFC settings
- Ensure "NFC and payment" is enabled
- Check for Samsung Pay conflicts

### Google Pixel Devices
- Verify NFC is enabled in Quick Settings
- Check for Google Pay settings
- Ensure no battery optimization is blocking NFC

### OnePlus Devices
- Check "NFC and payment" in settings
- Verify OxygenOS NFC compatibility
- Check for OnePlus Pay conflicts

## Alternative Solutions

### 1. Use Different NFC Position
- Try different areas of the device
- Some devices have NFC readers in specific locations
- Check device manual for NFC reader position

### 2. Remove Phone Case
- Some phone cases block NFC signals
- Remove case temporarily for testing
- Use NFC-compatible cases

### 3. Check for Interference
- Move away from electronic devices
- Avoid metal surfaces
- Check for magnetic interference

## Contact Support

If issues persist after trying all solutions:

1. **Collect Information:**
   - Device model and Android version
   - App version
   - Error messages and logs
   - Steps to reproduce the issue

2. **Report Issue:**
   - Include all collected information
   - Describe the exact behavior
   - Mention any error messages

## Prevention Tips

1. **Keep App Updated:**
   - Install latest app versions
   - Update Android system regularly

2. **Maintain Device:**
   - Keep NFC area clean
   - Avoid physical damage to NFC components
   - Regular device restarts

3. **Use Compatible Cards:**
   - Ensure cards are genuine CEPAS cards
   - Check card expiration dates
   - Verify card activation status

## Technical Details

### NFC Protocol Support
The app requires:
- ISO-DEP (ISO 14443-4) support
- APDU command support
- CEPAS protocol compatibility

### Minimum Requirements
- Android 5.0 (API 21) or higher
- NFC hardware support
- ISO-DEP technology support

### Supported Card Types
- CEPAS Purse 3 cards
- Singapore transit cards
- Compatible smart cards with CEPAS support
