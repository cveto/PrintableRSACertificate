PrintableRSACertificate
=======================

An Android application which finds an X.509 certificate and a private key inside a PKCS12 container and puts it in a QR code.
Needed for Digital Inkan application (in my repository) or for backuping X509 certificates.

Tested on X509 certificates that are exported to PKCS12 container with Windows7. 
Password protected. 
Private key is reduced to the private modoulus and private exponent only. 
Before it is converted to a QR code, it is encrypted with AES-256. Key is derived from the same password as you prided when extracting the PKCS12 using PBKDF2 (PBKDF2WithHmacSHA1)

End Result:
X509 Certificate in PEM QR and AES-256 encrypted private key (modulus and exponent only)
![alt tag](https://docs.google.com/uc?id=1_-0AASqzS4yeHqWFUkn_PQ62KtYWBHen)
![alt tag](https://docs.google.com/uc?id=1_ZG7Rp1wcR4NZmuwkCzLsGVO7Hg6qKqO)

Testing certificate and key in PKCS12 format: (lupus12345.p12 Password: 12345)
Compiled APP for installing the APP directly on the phone: (PrintableRSA.apk)
  https://drive.google.com/folderview?id=0B_9mRsWNDu2rOGE2SHVnZTVUNWM
  
Screenshots:
  Using:
![alt tag](https://docs.google.com/uc?id=1f6lXkCqtopvaQPBRfo7GzAJOSXQ_qXeH)
  Printing:
![alt tag](https://docs.google.com/uc?id=1vLFaGnzz_6HPdQk8Yq1K_cXlHWcOWBYo)

