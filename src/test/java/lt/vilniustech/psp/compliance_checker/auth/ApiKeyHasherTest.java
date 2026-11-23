package lt.vilniustech.psp.compliance_checker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyHasherTest {

    @Test
    void hashIsSha256AsLowercaseHex()
    {
        // Official SHA-256 test vector for the text "abc". Lowercase hex is also what the seed's
        // encode(sha256(...), 'hex') stores in firms.api_key_hash.
        String hash = ApiKeyHasher.hash("abc");

        assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
