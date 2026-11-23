package lt.vilniustech.psp.compliance_checker.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Turns an API key into the SHA-256 text that is stored in firms.api_key_hash.
 * The database never sees the plain key, and the hash cannot be turned back into the key.
 */
public final class ApiKeyHasher {

    private ApiKeyHasher()
    {
    }
    public static String hash(String apiKey)
    {
        try
        {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException e)
        {
            // Every Java platform must provide SHA-256, so this can't happen in practice.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
