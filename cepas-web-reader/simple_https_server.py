#!/usr/bin/env python3
"""
Simple HTTPS Server for CEPAS Web Reader
Uses Python's built-in libraries only
"""

import http.server
import socketserver
import ssl
import os
import sys

def create_simple_cert():
    """Create a simple self-signed certificate using Python's ssl module"""
    try:
        import subprocess
        
        # Try to use OpenSSL if available
        result = subprocess.run([
            'openssl', 'req', '-x509', '-newkey', 'rsa:4096',
            '-keyout', 'key.pem', '-out', 'cert.pem',
            '-days', '365', '-nodes',
            '-subj', '/C=US/ST=State/L=City/O=CEPAS/CN=localhost'
        ], capture_output=True, text=True)
        
        if result.returncode == 0:
            print("✅ Certificate created using OpenSSL")
            return True
        else:
            print("⚠️  OpenSSL not available, using Python's built-in SSL")
            return False
            
    except FileNotFoundError:
        print("⚠️  OpenSSL not found, using Python's built-in SSL")
        return False

def run_https_server(port=8443):
    """Run the HTTPS server"""
    
    # Try to create certificate
    cert_created = create_simple_cert()
    
    # Check if certificate files exist
    cert_file = 'cert.pem'
    key_file = 'key.pem'
    
    if not (os.path.exists(cert_file) and os.path.exists(key_file)):
        print("❌ Certificate files not found!")
        print("Please install OpenSSL or use one of the alternative methods below.")
        print("\nAlternative methods:")
        print("1. Install OpenSSL: https://slproweb.com/products/Win32OpenSSL.html")
        print("2. Use Node.js: npx http-server -S -C cert.pem -K key.pem")
        print("3. Use Live Server (VS Code extension)")
        return
    
    # Create HTTPS server
    handler = http.server.SimpleHTTPRequestHandler
    
    with socketserver.TCPServer(("", port), handler) as httpd:
        # Wrap socket with SSL
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain(cert_file, key_file)
        httpd.socket = context.wrap_socket(httpd.socket, server_side=True)
        
        print(f"🚀 HTTPS Server running at: https://localhost:{port}")
        print(f"📱 Access the CEPAS Web Reader in your browser")
        print(f"⚠️  Accept the security warning (self-signed certificate)")
        print(f"🛑 Press Ctrl+C to stop the server")
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n🛑 Server stopped")

if __name__ == "__main__":
    port = 8443
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print("Invalid port number. Using default port 8443")
    
    run_https_server(port)
