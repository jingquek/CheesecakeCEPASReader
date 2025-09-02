#!/usr/bin/env python3
"""
HTTPS Server for CEPAS Web Reader
This script creates a simple HTTPS server for local development
"""

import http.server
import socketserver
import ssl
import os
import sys
from pathlib import Path

def create_self_signed_cert():
    """Create a self-signed certificate for local development"""
    try:
        import subprocess
        
        # Check if mkcert is available (better for local development)
        try:
            subprocess.run(['mkcert', '--version'], capture_output=True, check=True)
            print("Using mkcert to create certificate...")
            subprocess.run(['mkcert', 'localhost'], check=True)
            return 'localhost.pem', 'localhost-key.pem'
        except (subprocess.CalledProcessError, FileNotFoundError):
            pass
        
        # Fallback: Create basic certificate using Python
        print("Creating self-signed certificate...")
        from cryptography import x509
        from cryptography.x509.oid import NameOID
        from cryptography.hazmat.primitives import hashes, serialization
        from cryptography.hazmat.primitives.asymmetric import rsa
        from datetime import datetime, timedelta
        
        # Generate private key
        private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048,
        )
        
        # Create certificate
        subject = issuer = x509.Name([
            x509.NameAttribute(NameOID.COUNTRY_NAME, "US"),
            x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "State"),
            x509.NameAttribute(NameOID.LOCALITY_NAME, "City"),
            x509.NameAttribute(NameOID.ORGANIZATION_NAME, "CEPAS Web Reader"),
            x509.NameAttribute(NameOID.COMMON_NAME, "localhost"),
        ])
        
        cert = x509.CertificateBuilder().subject_name(
            subject
        ).issuer_name(
            issuer
        ).public_key(
            private_key.public_key()
        ).serial_number(
            x509.random_serial_number()
        ).not_valid_before(
            datetime.utcnow()
        ).not_valid_after(
            datetime.utcnow() + timedelta(days=365)
        ).add_extension(
            x509.SubjectAlternativeName([
                x509.DNSName("localhost"),
                x509.IPAddress("127.0.0.1"),
            ]),
            critical=False,
        ).sign(private_key, hashes.SHA256())
        
        # Write certificate and key
        with open("cert.pem", "wb") as f:
            f.write(cert.public_bytes(serialization.Encoding.PEM))
        
        with open("key.pem", "wb") as f:
            f.write(private_key.private_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PrivateFormat.PKCS8,
                encryption_algorithm=serialization.NoEncryption()
            ))
        
        return 'cert.pem', 'key.pem'
        
    except ImportError:
        print("Error: cryptography library not found.")
        print("Install it with: pip install cryptography")
        return None, None

def run_https_server(port=8443):
    """Run the HTTPS server"""
    
    # Create certificate if it doesn't exist
    cert_file = 'cert.pem'
    key_file = 'key.pem'
    
    if not (os.path.exists(cert_file) and os.path.exists(key_file)):
        cert_file, key_file = create_self_signed_cert()
        if not cert_file:
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
