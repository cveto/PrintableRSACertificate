PrintableRSACertificate
=======================

An Android application which finds an X.509 certificate and a private key inside a PKCS12 container and puts it in a QR code.
Needed for Digital Inkan application (in my repository) or for backuping X509 certificates.

Tested on X509 certificates that are exported to PKCS12 container with Windows7. Password protected. Private key is reduced to the private modoulus and private exponent only. Before it is converted to a QR code, it is encrypted with AES-256. Key is derived from the same password as you prided when extracting the PKCS12 using PBKDF2 (PBKDF2WithHmacSHA1)

Screenshots:
![alt tag](https://lh3.googleusercontent.com/lIlBcjNLu92RdAb1MCFhOD_gVrOIrEgyMsuEoExqUz4wO1Pj7uuhj5UJsuU6hr9MOuSuEw=w1656-h786)

