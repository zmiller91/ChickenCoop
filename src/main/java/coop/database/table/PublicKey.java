package coop.database.table;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Getter
@Setter
@Entity
@Table(name = "public_keys")
public class PublicKey {
    @Id
    @Column(name = "public_key_id")
    private long id;

    @Lob
    private byte[] key;

    public java.security.PublicKey getPublicKey() {
        try {
            String strkey = new String(key);
            byte[] bytes = Base64.getDecoder().decode(key);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
