# p2p-naming

Distributed name resolution in a ring of six nodes (P0 to P5) over TCP, with no central server. Every hop uses hybrid crypto: an RSA handshake carries the AES and HMAC keys (plus the connecting node's RSA public key), then the search is AES/CBC encrypted and authenticated with HMAC-SHA256, and the HMAC is signed by encrypting it with the sender's RSA private key (`RSA/ECB/PKCS1Padding`). Each node decrypts it, checks the HMAC and its signature, acknowledges it, and re-encrypts it for the next node. The node that owns the file opens its own secure connection straight back to the node that asked.

Node `Pi` listens on port `8881 + i`, forwards to `P(i+1) % 6`, and owns `arquivo(i*10+1).ext` to `arquivo(i*10+10).ext`. Together the nodes hold 60 files.

Quick:
- Compile: `javac -encoding UTF-8 -d bin src/*.java`
- Run six terminals with `java -cp bin Main`, answering the prompt with the node id `0` to `5`

Node commands:
- `arquivo35.ext`: search for a file around the ring
- `corrupt arquivo55.ext`: send the search to the next node with one bit flipped (that node drops it and logs the HMAC failure)
- `sair`: exit

Names are validated before any connection: anything that isn't `arquivo<N>.ext` with N from 1 to 60 prints `Erro na entrada de dados. Tente outra vez!` and nothing is sent.

Test cases:
1. P0 `arquivo35.ext`: P0 → P1 → P2 → P3 finds it, and P3 answers P0 directly over a new encrypted connection.
2. P1 `arquivo99.ext` and P4 `arquivoABC.ext`: error text, nothing sent.
3. P4 `corrupt arquivo55.ext`: P5 logs the integrity failure and doesn't forward, and P4 prints that it got no reply (every hop sends an encrypted `ACK` after a valid HMAC).
