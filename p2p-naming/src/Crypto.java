import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RSA = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String AES = "AES/CBC/PKCS5Padding";
    private static final String SIGN = "RSA/ECB/PKCS1Padding";
    private static final int RSA_BYTES = 256; // RSA-2048: bloco cifrado e assinatura têm 256 bytes

    // Carrega chaves AES e HMAC de uma sessão segura, a chave privada RSA local (assina)
    // e a chave pública RSA do outro lado (verifica).
    public static class Session {
        final byte[] aesKey;
        final byte[] macKey;
        final PrivateKey mine;
        PublicKey peer;

        Session(byte[] aesKey, byte[] macKey, PrivateKey mine, PublicKey peer) {
            this.aesKey = aesKey;
            this.macKey = macKey;
            this.mine = mine;
            this.peer = peer;
        }
    }

    // Gera par de chaves RSA
    public static KeyPair newKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    // Cria uma sessão com chaves aleatórias (AES de 16 bytes e HMAC de 32 bytes).
    // "mine" é o par RSA do cliente, usado para assinar o que ele envia.
    public static Session newSession(KeyPair mine) {
        byte[] aes = new byte[16];
        byte[] mac = new byte[32];
        RANDOM.nextBytes(aes);
        RANDOM.nextBytes(mac);
        return new Session(aes, mac, mine.getPrivate(), null);
    }

    // Cifra as chaves da sessão com a chave pública RSA do servidor e anexa a chave pública
    // do cliente, para o servidor verificar as assinaturas dele.
    public static byte[] wrapKeys(byte[] publicKey, KeyPair mine, Session session) throws GeneralSecurityException {
        session.peer = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.ENCRYPT_MODE, session.peer);
        return concat(cipher.doFinal(concat(session.aesKey, session.macKey)), mine.getPublic().getEncoded());
    }

    // Decifra as chaves da sessão com a chave privada RSA, separa AES e HMAC e lê a chave
    // pública do cliente.
    public static Session unwrapKeys(PrivateKey key, byte[] wrapped) throws GeneralSecurityException {
        if (wrapped.length <= RSA_BYTES) {
            throw new GeneralSecurityException("invalid session keys");
        }
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] raw = cipher.doFinal(wrapped, 0, RSA_BYTES);
        if (raw.length != 48) {
            throw new GeneralSecurityException("invalid session keys");
        }
        PublicKey peer = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Arrays.copyOfRange(wrapped, RSA_BYTES, wrapped.length)));
        return new Session(Arrays.copyOfRange(raw, 0, 16), Arrays.copyOfRange(raw, 16, 48), key, peer);
    }

    // Cifra o texto com AES e anexa o HMAC assinado: o HMAC cifrado com a chave privada RSA local.
    public static byte[] seal(Session session, String text) throws GeneralSecurityException {
        byte[] iv = new byte[16];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(session.aesKey, "AES"), new IvParameterSpec(iv));
        byte[] body = concat(iv, cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        byte[] tag = hmac(session, body);
        Cipher signer = Cipher.getInstance(SIGN);
        signer.init(Cipher.ENCRYPT_MODE, session.mine); // assinatura: cifra com a chave PRIVADA
        return concat(body, signer.doFinal(tag));
    }

    // Decifra a assinatura com a chave pública do outro lado, compara o HMAC recebido com o
    // calculado localmente e, se estiver correto, decifra a mensagem e devolve o texto.
    public static String open(Session session, byte[] message) throws GeneralSecurityException {
        if (message.length < 16 + 16 + RSA_BYTES) {
            throw new GeneralSecurityException("message too short");
        }
        int end = message.length - RSA_BYTES;
        byte[] body = Arrays.copyOfRange(message, 0, end);
        Cipher verifier = Cipher.getInstance(SIGN);
        verifier.init(Cipher.DECRYPT_MODE, session.peer); // verificação: decifra com a chave PÚBLICA
        byte[] signed;
        try {
            signed = verifier.doFinal(message, end, RSA_BYTES);
        } catch (GeneralSecurityException e) {
            throw new GeneralSecurityException("invalid RSA signature");
        }
        if (!MessageDigest.isEqual(signed, hmac(session, body))) {
            throw new GeneralSecurityException("invalid HMAC");
        }
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(session.aesKey, "AES"),
                new IvParameterSpec(Arrays.copyOfRange(body, 0, 16)));
        return new String(cipher.doFinal(body, 16, body.length - 16), StandardCharsets.UTF_8);
    }

    // TESTE: inverte um bit do texto cifrado para o HMAC falhar no destino.
    public static byte[] corrupt(byte[] message) {
        byte[] copy = message.clone();
        copy[16] ^= 1;
        return copy;
    }

    // Calcula o HMAC-SHA256 dos dados usando a chave HMAC da sessão.
    private static byte[] hmac(Session session, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(session.macKey, "HmacSHA256"));
        return mac.doFinal(data);
    }

    // Concatena dois arrays de bytes
    static byte[] concat(byte[] a, byte[] b) {
        byte[] result = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
