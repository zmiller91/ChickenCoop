//package coop.remote;
//
//import com.amazonaws.services.iot.AWSIot;
//import com.amazonaws.services.iot.AWSIotClientBuilder;
//import com.amazonaws.services.iot.client.AWSIotException;
//import com.amazonaws.services.iot.client.AWSIotMessage;
//import com.amazonaws.services.iot.client.AWSIotMqttClient;
//import com.amazonaws.services.iot.client.AWSIotQos;
//import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
//import com.amazonaws.services.iot.model.*;
//import com.google.gson.Gson;
//import coop.shared.mqtt.PublishMessage;
//import coop.shared.pi.config.CoopState;
//import coop.shared.pi.config.IotShadowRequest;
//import coop.shared.pi.config.IotState;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang3.RandomStringUtils;
//
//import java.io.File;
//import java.nio.charset.StandardCharsets;
//
//public class ProvisionDevice {
//
//    public static void main(String... args) throws Exception {
//
//        String outputPath = "C:\\Users\\zmiller\\IdeaProjects\\ChickenCoop\\certs\\things\\";
//        String thingName = "pi-" + RandomStringUtils.randomAlphabetic(7).toUpperCase();
//        String thingClientId = "client-" + thingName;
//
//        AWSIot client = AWSIotClientBuilder.defaultClient();
//        CreateThingRequest createThingRequest = new CreateThingRequest();
//        createThingRequest.setThingName(thingName);
//
//        CreateThingResult result = client.createThing(createThingRequest);
//
//        CreateKeysAndCertificateRequest certificateRequest = new CreateKeysAndCertificateRequest();
//        certificateRequest.setSetAsActive(true);
//        CreateKeysAndCertificateResult certificateResult = client.createKeysAndCertificate(certificateRequest);
//        String certificatePem = certificateResult.getCertificatePem();
//        KeyPair kp = certificateResult.getKeyPair();
//        String privateKey = kp.getPrivateKey();
//        String publicKey = kp.getPublicKey();
//
//        String piContextPath = outputPath + thingName;
//        String certificateFile = piContextPath + "\\certificate.pem.crt";
//        String privateKeyFile = piContextPath + "\\private.pem.key";
//        String publicKeyFile = piContextPath + "\\public.pem.key";
//        String contextFile = piContextPath + "\\context.txt";
//
//        mkdir(piContextPath);
//        write(piContextPath + "\\" + certificateResult.getCertificateId(), "");
//        write(certificateFile, certificatePem);
//        write(privateKeyFile, privateKey);
//        write(publicKeyFile, publicKey);
//        write(contextFile, context(privateKeyFile, publicKeyFile, certificateFile, thingClientId, thingName));
//
//        CreatePolicyRequest policyRequest = new CreatePolicyRequest();
//        policyRequest.setPolicyName("Pi-Policy-" + thingName);
//        policyRequest.setPolicyDocument(policy(thingName, thingClientId));
//        client.createPolicy(policyRequest);
//
//        AttachPolicyRequest attachPolicyRequest = new AttachPolicyRequest();
//        attachPolicyRequest.setPolicyName(policyRequest.getPolicyName());
//        System.out.println(certificateResult.getCertificateArn());
//        attachPolicyRequest.setTarget(certificateResult.getCertificateArn());
//        client.attachPolicy(attachPolicyRequest);
//
//        AttachThingPrincipalRequest principalRequest = new AttachThingPrincipalRequest();
//        principalRequest.setPrincipal(certificateResult.getCertificateArn());
//        principalRequest.setThingName(thingName);
//        client.attachThingPrincipal(principalRequest);
//
//        String clientEndpoint = "a2wakfrvfa97is-ats.iot.us-east-1.amazonaws.com";
//
//        SampleUtil.KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
//        AWSIotMqttClient mqtt = new AWSIotMqttClient(clientEndpoint, thingClientId, pair.keyStore, pair.keyPassword);
//        mqtt.connect();
//
//        CoopState config = new CoopState();
//        IotState state = new IotState();
//        state.setDesired(config);
//        IotShadowRequest shadowRequest = new IotShadowRequest();
//        shadowRequest.setState(state);
//
//        String topic = String.format("$aws/things/%s/shadow/update", thingName);
//        PublishMessage createShadowRequest = new PublishMessage(topic, new Gson().toJson(shadowRequest));
//        mqtt.publish(createShadowRequest);
//
//    }
//
//    private static String context(String privateKey, String publicKey, String cert, String clientId, String thingName) {
//        return String.format("""
//                private_key=%s
//                public_key=%s
//                cert_key=%s
//                endpoint=a2wakfrvfa97is-ats.iot.us-east-1.amazonaws.com
//                client_id=%s
//                shadow_name=%s""", privateKey, publicKey, cert, clientId, thingName);
//    }
//
//    private static void mkdir(String path) throws Exception {
//        FileUtils.forceMkdir(new File(path));
//    }
//
//    private static void write(String fileName, String data) throws Exception {
//        File file = new File(fileName);
//        FileUtils.writeStringToFile(file, data, StandardCharsets.UTF_8);
//    }
//
//    private static String policy(String coopName, String clientName) {
//        return String.format("{\n" +
//                "  \"Version\": \"2012-10-17\",\n" +
//                "  \"Statement\": [\n" +
//                "    {\n" +
//                "      \"Effect\": \"Allow\",\n" +
//                "      \"Action\": [\n" +
//                "        \"iot:Publish\",\n" +
//                "        \"iot:Receive\",\n" +
//                "        \"iot:PublishRetain\"\n" +
//                "      ],\n" +
//                "      \"Resource\": \"arn:aws:iot:us-east-1:547228847576:topic/$aws/things/%s/*\"\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"Effect\": \"Allow\",\n" +
//                "      \"Action\": [\n" +
//                "        \"iot:Publish\"\n" +
//                "      ],\n" +
//                "      \"Resource\": \"arn:aws:iot:us-east-1:547228847576:topic/hub_event\"\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"Effect\": \"Allow\",\n" +
//                "      \"Action\": \"iot:Subscribe\",\n" +
//                "      \"Resource\": \"arn:aws:iot:us-east-1:547228847576:topicfilter/$aws/things/%s/*\"\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"Effect\": \"Allow\",\n" +
//                "      \"Action\": \"iot:Connect\",\n" +
//                "      \"Resource\": \"arn:aws:iot:us-east-1:547228847576:client/%s\"\n" +
//                "    }\n" +
//                "  ]\n" +
//                "}", coopName, coopName, clientName);
//    }
//}
