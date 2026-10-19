# cdn

Content Delivery Network simulation with four components: one Origin, four Edges (one per region), one Router (GeoDNS) and any number of Clients. Every connection uses hybrid crypto: an RSA handshake carries the AES and HMAC keys (plus the client's RSA public key), then each message is AES/CBC encrypted and authenticated with HMAC-SHA256, and the HMAC is signed by encrypting it with the sender's RSA private key (`RSA/ECB/PKCS1Padding`). The Router uses UDP (same handshake as `mini-dns`), everything else uses TCP.

- Origin (TCP 8000): serves `arquivo1.ext` to `arquivo1000.ext` and answers `GET /arquivoN.ext` with `200 OK` and `Length: 1024`. The files are simulated: only the names exist, no content is sent.
- Edges: one per region, `NORTE` on TCP 8001, `SUL` on 8002, `LESTE` on 8003, `OESTE` on 8004. Every log line is tagged `[Edge SUL (8002)]`, and replies to clients carry an `Edge: SUL (8002)` line. Each edge has its own in-memory cache with a 10 s TTL (it remembers when each file was fetched). A HIT answers right away; a MISS asks the Origin over a new encrypted connection and saves the file in the cache.
- Router (UDP 8081): answers `WHERE:<region>:<file>` with the address of the edge for that region (`OK:127.0.0.1:8002` for `SUL`).
- Client: lives in one region (`NORTE`, `SUL`, `LESTE` or `OESTE`, given as an argument or typed at startup). It asks the Router where to go, then sends `GET /<file>` plus a `Region: <region>` line to that Edge (so the edge logs `client in SUL`) and prints the status, `Cache`, `Length` and the time in ms.

Reply format: `200 OK\nEdge: SUL (8002)\nCache: HIT\nLength: 1024` (errors add a blank line and the error text).

Quick:
- Compile: `javac -encoding UTF-8 -d bin src/*/*.java`
- Run each component in its own terminal, in this order:
  - Origin: `java -cp bin Origin.Origin [port]` (default 8000)
  - Edges: `java -cp bin Edge.Edge 8001` (NORTE), `8002` (SUL), `8003` (LESTE), `8004` (OESTE), one terminal each (without an argument it asks for the port)
  - Router: `java -cp bin Router.Router [port]` (default 8081)
  - Clients: `java -cp bin Client.Client [host] [port] [NORTE|SUL|LESTE|OESTE]` (without a region it asks for one), one terminal per client

The Router defaults to 8081 instead of 5353 because Linux usually runs mDNS/avahi on 5353. To use another port, pass the same one to the Router and the Clients: `java -cp bin Router.Router 5354` and `java -cp bin Client.Client 127.0.0.1 5354`.

Origin commands:
- `invalidate arquivo30.ext`: send an encrypted `INVALIDATE /arquivo30.ext` to the edge
- `sair`: exit

Client commands:
- `arquivo20.ext`: download a file
- `corrupt arquivo20.ext`: send the `GET` with one bit flipped (the edge logs an alert and closes the connection)
- `sair`: exit

Test cases:
1. Client `arquivo20.ext` twice: the first reply is `Cache: MISS` (the edge fetches from the Origin), the second is `Cache: HIT` and faster.
2. Client `arquivo30.ext` (MISS, then HIT), then Origin `invalidate arquivo30.ext`: the edge removes it, and the next `arquivo30.ext` is a MISS. Waiting more than 10 s after a file is cached also gives a MISS (TTL expired).
3. Client `arquivoXYZ.ext`: the edge answers `400 Bad Request` and the client prints `Erro na entrada de dados. Tente outra vez!`. Nothing shows up in the Origin log.
4. Client `corrupt arquivo20.ext`: the edge logs `ALERT: integrity failure (invalid HMAC)` and closes the connection. The next `arquivo20.ext` is still a HIT (the cache wasn't touched).
