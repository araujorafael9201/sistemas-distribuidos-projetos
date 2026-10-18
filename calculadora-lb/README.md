# calculadora-lb

Service discovery with load balancing over TCP (Directory, Calculator, Client). Every connection uses hybrid crypto: an RSA handshake carries the AES and HMAC keys, then each message is AES/CBC encrypted and authenticated with HMAC-SHA256.

Services: `soma`, `subtração`, `multiplicação`, `divisão`.

Quick:
- Compile: `javac -encoding UTF-8 -d bin src/*/*.java`
- Run each component in its own terminal, in this order:
  - Directory: `java -cp bin Directory.Directory [port]` (default 8000)
  - Calculators S1, S2, S3: `java -cp bin Calculator.Calculator`, which asks for the name (`S1`), the port (`9001`) and whether to corrupt every reply (`s` for tamper mode)
  - Clients C1, C2, C3: `java -cp bin Client.Client [host] [port]`

Client commands:
- `soma 10 20` (also `subtração`, `multiplicação`, `divisão`): look up the service, then call a server
- `sair`: exit

The calculators don't compute anything: each one answers `<name> handled "<request>"`, so the logs show which node took each call.

The Directory returns the server list rotated by one global round-robin counter. The client tries the servers in that order, skips unreachable ones, and aborts the call if a reply fails the HMAC check.

Test order (every valid lookup advances the counter, so S3 comes first on lookups 3, 6, 9, ...):
1. C1 `soma 10 20`: S1 handles it.
2. C2 `multiplicação 5 4`: S2 handles it.
3. C3 `modulo 1 2`: prints `Erro na entrada de dados. Tente outra vez!` (doesn't advance the counter).
4. Kill S3, then C3 `soma 1 2`: S3 is skipped as unavailable and S1 handles it.
5. Restart S3 and answer `s` to the tamper question, then make calls until a lookup lists S3 first: the client discards the reply (invalid HMAC).
