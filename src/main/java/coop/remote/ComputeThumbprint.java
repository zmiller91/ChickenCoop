package coop.remote;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * When communicating with AWS IOT the principal we receive from IoT is the thumbprint of the certificate. This is how
 * we can authenticate callers on the shared MQTT topics. This code calculates that thumbprint so it can be stored
 * in the database.
 */
public class ComputeThumbprint {

    public static void main(String... args) throws Exception {
        System.out.println(thumbprintSha256FromPemFile(Path.of("C:\\Users\\zmiller\\IdeaProjects\\ChickenCoop\\certs\\things\\pi-PQTMXIB\\certificate.pem.crt")));
    }


    public static String thumbprintSha256FromPemFile(Path pemPath) throws Exception {

        String pem = Files.readString(pemPath, StandardCharsets.UTF_8);

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))
        );

        byte[] der = cert.getEncoded();

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest(der);

        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) hex.append(String.format("%02x", b));
        return hex.toString(); // lowercase hex, no colons (matches principal())
    }


}
