# Running the CEPAS Web Reader with HTTPS

Since the Web NFC API requires HTTPS, here are several ways to run the webapp locally:

## **Method 1: Install OpenSSL (Recommended)**

### **Step 1: Install OpenSSL**
Download and install OpenSSL for Windows:
- Go to: https://slproweb.com/products/Win32OpenSSL.html
- Download "Win64 OpenSSL v3.x.x" (latest version)
- Install it and add to PATH

### **Step 2: Generate Certificate**
```bash
# In the cepas-web-reader directory
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/C=US/ST=State/L=City/O=CEPAS/CN=localhost"
```

### **Step 3: Run HTTPS Server**
```bash
# Using Python
python simple_https_server.py

# Or using Node.js
npx http-server -S -C cert.pem -K key.pem
```

## **Method 2: Using Node.js (Easiest)**

### **Step 1: Install Node.js**
Download from: https://nodejs.org/

### **Step 2: Install http-server**
```bash
npm install -g http-server
```

### **Step 3: Generate Certificate (if needed)**
```bash
# If you have OpenSSL installed
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes -subj "/C=US/ST=State/L=City/O=CEPAS/CN=localhost"
```

### **Step 4: Run Server**
```bash
# With existing certificate
http-server -S -C cert.pem -K key.pem

# Or let http-server create one automatically
http-server -S
```

## **Method 3: Using VS Code Live Server**

### **Step 1: Install Live Server Extension**
1. Open VS Code
2. Go to Extensions (Ctrl+Shift+X)
3. Search for "Live Server"
4. Install the extension by Ritwick Dey

### **Step 2: Configure Live Server for HTTPS**
1. Open VS Code settings (Ctrl+,)
2. Search for "live server"
3. Find "Live Server › Settings: Https"
4. Enable it and configure certificate paths

### **Step 3: Run**
1. Right-click on `index.html`
2. Select "Open with Live Server"
3. It will open in HTTPS mode

## **Method 4: Using Python with mkcert (Best for Development)**

### **Step 1: Install mkcert**
```bash
# Using Chocolatey (Windows)
choco install mkcert

# Using Homebrew (macOS)
brew install mkcert

# Or download from: https://github.com/FiloSottile/mkcert/releases
```

### **Step 2: Install Root Certificate**
```bash
mkcert -install
```

### **Step 3: Create Certificate for localhost**
```bash
mkcert localhost
```

### **Step 4: Run Server**
```bash
python simple_https_server.py
```

## **Method 5: Using ngrok (For Testing on Mobile)**

### **Step 1: Install ngrok**
Download from: https://ngrok.com/

### **Step 2: Run HTTP Server**
```bash
# Start a simple HTTP server
python -m http.server 8000
```

### **Step 3: Create HTTPS Tunnel**
```bash
ngrok http 8000
```

### **Step 4: Access via ngrok URL**
Use the HTTPS URL provided by ngrok (e.g., `https://abc123.ngrok.io`)

## **Quick Start Commands**

### **For Windows (PowerShell)**
```powershell
# Navigate to the directory
cd cepas-web-reader

# Method 1: Using Python (if OpenSSL is installed)
python simple_https_server.py

# Method 2: Using Node.js
npx http-server -S

# Method 3: Using Python HTTP (then ngrok)
python -m http.server 8000
# Then in another terminal: ngrok http 8000
```

### **For macOS/Linux**
```bash
# Navigate to the directory
cd cepas-web-reader

# Method 1: Using Python
python3 simple_https_server.py

# Method 2: Using Node.js
npx http-server -S

# Method 3: Using mkcert (recommended)
mkcert localhost
python3 simple_https_server.py
```

## **Troubleshooting**

### **Certificate Issues**
- **"Certificate not trusted"**: Accept the security warning in your browser
- **"Certificate expired"**: Regenerate the certificate
- **"Invalid certificate"**: Make sure the certificate is for `localhost`

### **Port Issues**
- **"Port already in use"**: Change the port number
  ```bash
  python simple_https_server.py 8444
  ```

### **Browser Issues**
- **"Web NFC not supported"**: Use Chrome 89+ or Edge 89+ on Android
- **"HTTPS required"**: Make sure you're accessing via `https://` not `http://`

### **NFC Issues**
- **"NFC permission denied"**: Enable NFC in device settings
- **"No NFC detected"**: Ensure NFC is enabled and card is near reader

## **Testing the App**

### **1. Desktop Testing (Simulation)**
- Open the app in any browser
- Click "Start NFC Scan"
- The app will show simulated data (no real NFC needed)

### **2. Mobile Testing (Real NFC)**
- Use ngrok or deploy to a real HTTPS server
- Access from your mobile device
- Enable NFC and grant permissions
- Test with a real CEPAS card

### **3. Browser Compatibility**
- **Chrome 89+** on Android: Full support
- **Edge 89+** on Android: Full support
- **Samsung Internet 15+**: Full support
- **Firefox**: Experimental support
- **Desktop browsers**: Limited NFC support

## **Security Notes**

- Self-signed certificates are only for development
- Never use self-signed certificates in production
- The app only reads public data from cards
- No sensitive data is stored or transmitted

## **Next Steps**

Once you have the app running:

1. **Test the interface**: Click "Start NFC Scan" to see the UI
2. **View simulated data**: See how the CAN ID calculation works
3. **Test on mobile**: Use ngrok or deploy to test real NFC
4. **Customize**: Modify the code to add your own features

## **Deployment Options**

For production use, consider:

1. **Static hosting**: Netlify, Vercel, GitHub Pages
2. **Cloud hosting**: AWS, Google Cloud, Azure
3. **VPS**: DigitalOcean, Linode, Vultr
4. **CDN**: Cloudflare, AWS CloudFront

All of these provide HTTPS by default and are suitable for hosting the webapp.
