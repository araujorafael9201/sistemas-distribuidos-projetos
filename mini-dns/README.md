# mini-dns

Tiny Java DNS over UDP (Server, Registrator, Client). Every message uses hybrid crypto: an RSA handshake carries the AES and HMAC keys (plus the client's RSA public key), then each datagram is AES/CBC encrypted and authenticated with HMAC-SHA256, and the HMAC is signed by encrypting it with the sender's RSA private key (`RSA/ECB/PKCS1Padding`).

Quick:
- Compile: `javac -d bin src/*.java`
- Run each component in its own terminal, in this order:
  - Server: `java -cp bin Server [port]` (default 8888)
  - Clients C1, C2, C3: `java -cp bin Client [host] [port]`
  - Registrator: `java -cp bin Registrator [host] [port]`

Client commands:
- `servidor1`: look up a name
- `corrupt servidor1`: send the lookup with one bit flipped (the server drops it and logs the HMAC failure)
- `tabela`: show the local table
- `sair`: exit

Registrator: asks for a name and an IPv4 address, sends `REGISTER`, and every client prints the update.
