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

    // Carrega chaves AES e HMAC de uma sessão segura
    public static class Session {
        final byte[] aesKey;
        final byte[] macKey;

        Session(byte[] aesKey, byte[] macKey) {
            this.aesKey = aesKey;
            this.macKey = macKey;
        }
    }

    // Gera par de chaves RSA
    public static KeyPair newKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    // Cria uma sessão com chaves aleatórias (AES de 16 bytes e HMAC de 32 bytes).
    public static Session newSession() {
        byte[] aes = new byte[16];
        byte[] mac = new byte[32];
        RANDOM.nextBytes(aes);
        RANDOM.nextBytes(mac);
        return new Session(aes, mac);
    }

    // Cifra as chaves da sessão com a chave pública RSA do servidor
    public static byte[] wrapKeys(byte[] publicKey, Session session) throws GeneralSecurityException {
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(concat(session.aesKey, session.macKey));
    }

    // Decifra as chaves da sessão com a chave privada RSA e separa AES e HMAC.
    public static Session unwrapKeys(PrivateKey key, byte[] wrapped) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] raw = cipher.doFinal(wrapped);
        if (raw.length != 48) {
            throw new GeneralSecurityException("invalid session keys");
        }
        return new Session(Arrays.copyOfRange(raw, 0, 16), Arrays.copyOfRange(raw, 16, 48));
    }

    // Cifra o texto com AES e anexa um HMAC
    public static byte[] seal(Session session, String text) throws GeneralSecurityException {
        byte[] iv = new byte[16];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(session.aesKey, "AES"), new IvParameterSpec(iv));
        byte[] body = concat(iv, cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        return concat(body, hmac(session, body));
    }

    // Confere o HMAC e, se estiver correto, decifra a mensagem e devolve o texto.
    public static String open(Session session, byte[] message) throws GeneralSecurityException {
        if (message.length < 16 + 16 + 32) {
            throw new GeneralSecurityException("message too short");
        }
        byte[] body = Arrays.copyOfRange(message, 0, message.length - 32);
        byte[] tag = Arrays.copyOfRange(message, message.length - 32, message.length);
        if (!MessageDigest.isEqual(tag, hmac(session, body))) {
            throw new GeneralSecurityException("invalid HMAC");
        }
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(session.aesKey, "AES"),
                new IvParameterSpec(Arrays.copyOfRange(body, 0, 16)));
        return new String(cipher.doFinal(body, 16, body.length - 16), StandardCharsets.UTF_8);
    }

    // TESTE: inverte um bit da mensagem para o HMAC falhar no destino.
    public static byte[] corrupt(byte[] message) {
        byte[] copy = message.clone();
        copy[copy.length / 2] ^= 1;
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
