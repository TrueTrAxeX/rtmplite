package org.rtmplite.main;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.rtmplite.utils.FileUtil;
import org.rtmplite.utils.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Handshake {

	private static final Logger log = LoggerFactory.getLogger(Handshake.class);
	
	private static final byte[] GENUINE_FMS_KEY = {
		(byte) 0x47, (byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20,
		(byte) 0x41, (byte) 0x64, (byte) 0x6f, (byte) 0x62, (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6c,
		(byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x69,
		(byte) 0x61, (byte) 0x20, (byte) 0x53, (byte) 0x65, (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72,
		(byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Media Server 001
		(byte) 0xf0, (byte) 0xee, (byte) 0xc2, (byte) 0x4a, (byte) 0x80, (byte) 0x68, (byte) 0xbe, (byte) 0xe8,
		(byte) 0x2e, (byte) 0x00, (byte) 0xd0, (byte) 0xd1, (byte) 0x02, (byte) 0x9e, (byte) 0x7e, (byte) 0x57,
		(byte) 0x6e, (byte) 0xec, (byte) 0x5d, (byte) 0x2d, (byte) 0x29, (byte) 0x80, (byte) 0x6f, (byte) 0xab,
		(byte) 0x93, (byte) 0xb8, (byte) 0xe6, (byte) 0x36, (byte) 0xcf, (byte) 0xeb, (byte) 0x31, (byte) 0xae};
	
	protected static final byte[] GENUINE_FP_KEY = {
		(byte) 0x47, (byte) 0x65, (byte) 0x6E, (byte) 0x75, (byte) 0x69, (byte) 0x6E, (byte) 0x65, (byte) 0x20,
		(byte) 0x41, (byte) 0x64, (byte) 0x6F, (byte) 0x62, (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6C,
		(byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x50, (byte) 0x6C, (byte) 0x61, (byte) 0x79,
		(byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Player 001
		(byte) 0xF0, (byte) 0xEE, (byte) 0xC2, (byte) 0x4A, (byte) 0x80, (byte) 0x68, (byte) 0xBE, (byte) 0xE8,
		(byte) 0x2E, (byte) 0x00, (byte) 0xD0, (byte) 0xD1, (byte) 0x02, (byte) 0x9E, (byte) 0x7E, (byte) 0x57,
		(byte) 0x6E, (byte) 0xEC, (byte) 0x5D, (byte) 0x2D, (byte) 0x29, (byte) 0x80, (byte) 0x6F, (byte) 0xAB,
		(byte) 0x93, (byte) 0xB8, (byte) 0xE6, (byte) 0x36, (byte) 0xCF, (byte) 0xEB, (byte) 0x31, (byte) 0xAE};	
	
	/** Modulus bytes from flazr */
	private static final byte[] DH_MODULUS_BYTES = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xc9, (byte) 0x0f, (byte) 0xda, (byte) 0xa2, (byte) 0x21,
			(byte) 0x68, (byte) 0xc2, (byte) 0x34, (byte) 0xc4, (byte) 0xc6, (byte) 0x62, (byte) 0x8b, (byte) 0x80,
			(byte) 0xdc, (byte) 0x1c, (byte) 0xd1, (byte) 0x29, (byte) 0x02, (byte) 0x4e, (byte) 0x08, (byte) 0x8a,
			(byte) 0x67, (byte) 0xcc, (byte) 0x74, (byte) 0x02, (byte) 0x0b, (byte) 0xbe, (byte) 0xa6, (byte) 0x3b,
			(byte) 0x13, (byte) 0x9b, (byte) 0x22, (byte) 0x51, (byte) 0x4a, (byte) 0x08, (byte) 0x79, (byte) 0x8e,
			(byte) 0x34, (byte) 0x04, (byte) 0xdd, (byte) 0xef, (byte) 0x95, (byte) 0x19, (byte) 0xb3, (byte) 0xcd,
			(byte) 0x3a, (byte) 0x43, (byte) 0x1b, (byte) 0x30, (byte) 0x2b, (byte) 0x0a, (byte) 0x6d, (byte) 0xf2,
			(byte) 0x5f, (byte) 0x14, (byte) 0x37, (byte) 0x4f, (byte) 0xe1, (byte) 0x35, (byte) 0x6d, (byte) 0x6d,
			(byte) 0x51, (byte) 0xc2, (byte) 0x45, (byte) 0xe4, (byte) 0x85, (byte) 0xb5, (byte) 0x76, (byte) 0x62,
			(byte) 0x5e, (byte) 0x7e, (byte) 0xc6, (byte) 0xf4, (byte) 0x4c, (byte) 0x42, (byte) 0xe9, (byte) 0xa6,
			(byte) 0x37, (byte) 0xed, (byte) 0x6b, (byte) 0x0b, (byte) 0xff, (byte) 0x5c, (byte) 0xb6, (byte) 0xf4,
			(byte) 0x06, (byte) 0xb7, (byte) 0xed, (byte) 0xee, (byte) 0x38, (byte) 0x6b, (byte) 0xfb, (byte) 0x5a,
			(byte) 0x89, (byte) 0x9f, (byte) 0xa5, (byte) 0xae, (byte) 0x9f, (byte) 0x24, (byte) 0x11, (byte) 0x7c,
			(byte) 0x4b, (byte) 0x1f, (byte) 0xe6, (byte) 0x49, (byte) 0x28, (byte) 0x66, (byte) 0x51, (byte) 0xec,
			(byte) 0xe6, (byte) 0x53, (byte) 0x81, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff };

    private static final BigInteger DH_MODULUS = new BigInteger(1, DH_MODULUS_BYTES);

    private static final BigInteger DH_BASE = BigInteger.valueOf(2); 
    
    private static final byte[] CLIENT_CONST = "Genuine Adobe Flash Player 001".getBytes();
    private static final byte[] SERVER_CONST = "Genuine Adobe Flash Media Server 001".getBytes();
    
	private static final int HANDSHAKE_SIZE = 1536;
	private static final int HANDSHAKE_SIZE_SERVER = (HANDSHAKE_SIZE * 2) + 1;
	
	private static final int DIGEST_LENGTH = 32;

	private static final int KEY_LENGTH = 128;
	
	private static KeyAgreement keyAgreement;
	
	private Random random = new Random();
	
	private Mac hmacSHA256;
	
	private Socket socket;
	
	private byte[] outgoingPublicKey;
	
	private byte[] outgoingDigest;
	
	private byte[] incomingDigest;
	
	private byte[] incomingPublicKey;
	
	private byte[] handshakeBytes;
	
	private byte[] swfVerificationBytes;
	
	private Cipher cipherOut;
	private Cipher cipherIn;
	
	private byte[] swfHash;
	private int swfSize;
	
	private int handshakeType = 3;
	
	public void setHandshakeType(int type) {
		handshakeType = type;
	}
	
	public Handshake(Socket socket) {
		
		log.trace("Handshake ctor");
		try {
			hmacSHA256 = Mac.getInstance("HmacSHA256");
		} catch (SecurityException e) {
			log.error("Security exception when getting HMAC", e);
		} catch (NoSuchAlgorithmException e) {
			log.error("HMAC SHA256 does not exist");
		}
		
		handshakeBytes = createHandshakeBytes();
		
		this.socket = socket;
	}

	private byte[] createHandshakeBytes() {

		byte[] bytes = new byte[HANDSHAKE_SIZE];
		
		// set timestamp
		bytes[0] = 0;
		bytes[1] = 0;
		bytes[2] = 0;
		bytes[3] = 0;
		
		// set flash player version 11.2.202.235
		bytes[4] = (byte) 0x80;
		bytes[5] = 0;
		bytes[6] = 7;
		bytes[7] = 2;
		
		// fill the rest with random bytes
		byte[] rndBytes = new byte[HANDSHAKE_SIZE - 8];
		random.nextBytes(rndBytes);
		
		// copy random bytes into our handshake array
		System.arraycopy(rndBytes, 0, bytes, 8, (HANDSHAKE_SIZE - 8));
		
		return bytes;
	}
	
	/**
	 * Creates a Diffie-Hellman key pair.
	 * 
	 * @return dh keypair
	 */
	private KeyPair generateKeyPair() {
		KeyPair keyPair = null;
		DHParameterSpec keySpec = new DHParameterSpec(DH_MODULUS, DH_BASE);
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
			keyGen.initialize(keySpec);
			keyPair = keyGen.generateKeyPair();
		    keyAgreement = KeyAgreement.getInstance("DH");
		    keyAgreement.init(keyPair.getPrivate());
		} catch (Exception e) {
			log.error("Error generating keypair", e);
		}
		return keyPair;
	}
	
	/**
	 * Run handshake method
	 * @throws Exception
	 */
	public void doHandshake() throws Exception {
		
		firstRequest();
		
		decodeServerResponse(receiveFirstHandshakeRawResponse());
		
		secondRequest();
	}
	
	private boolean decodeServerResponse(byte[] response) {
		
		// minus one for the first byte which is not included (handshake type byte)
		byte[] handledResponse = new byte[HANDSHAKE_SIZE_SERVER - 1];
		int pos = 1;
		
		for(int i=0; i<HANDSHAKE_SIZE_SERVER - 1; i++) {
			handledResponse[i] = response[pos++];
		}

		log.debug("response: {}", Hex.toHexString(handledResponse));
		
		byte[] part1 = new byte[HANDSHAKE_SIZE];
		
		for(int i=0; i<HANDSHAKE_SIZE; i++) {
			part1[i] = handledResponse[i]; 
		}
		
		log.debug("Server response part 1: {}", part1);
		log.info("Processing server response for encryption");
		
		byte[] serverTime;
		serverTime = getFourBytesFrom(part1, 0);
		
		log.debug("Server time: {}",  ((serverTime[0] << 24) + (serverTime[1] << 16) + (serverTime[2] << 8) + (serverTime[3] << 0)));
		
		byte[] serverVersion = getFourBytesFrom(part1, 4);
		log.debug("Server version: {}", ((serverVersion[0] << 24) + (serverVersion[1] << 16) + (serverVersion[2] << 8) + (serverVersion[3] << 0)));
		
		byte[] digestPointer = getFourBytesFrom(part1, 8); // position 8
		int digestOffset = calculateOffset(digestPointer, 728, 12);
		
		int messageLength = HANDSHAKE_SIZE - DIGEST_LENGTH;
		byte[] message = new byte[messageLength];
		
		pos = 0;
		
		for(int i=0; i<digestOffset; i++) {
			message[pos++] = part1[i];
		}
		
		int afterDigestOffset = digestOffset + DIGEST_LENGTH;
	
		for(int i=afterDigestOffset; i<HANDSHAKE_SIZE; i++) {
			message[pos++] = part1[i];
		}
		
		byte[] digest = calculateHMAC_SHA256(message, SERVER_CONST);
		
		log.debug("Digest: {}", Hex.toHexString(digest));
		
		incomingDigest = new byte[DIGEST_LENGTH];
		
		pos = 0;
		
		for(int i=digestOffset; i<DIGEST_LENGTH+digestOffset; i++) {
			incomingDigest[pos++] = part1[i]; 
		}

		log.debug("Incoming digest: {}", Hex.toHexString(incomingDigest));
		
		incomingPublicKey = new byte[128];
		if (Arrays.equals(digest, incomingDigest)) {
			log.info("Type 0 digest comparison success");
			
			byte[] dhPointer = getFourBytesFrom(part1, HANDSHAKE_SIZE - 4);
			int dhOffset = calculateOffset(dhPointer, 632, 772);
			
			pos = 0;
			
			for(int i=dhOffset; i<incomingPublicKey.length+dhOffset; i++) {
				incomingPublicKey[pos++] = part1[i];
			}
		} else {
			throw new RuntimeException("Type 1 digest comparison is not supported. Please wait new version of library...");
		}
		
		log.debug("server public key: {}", Hex.toHexString(incomingPublicKey));
		byte[] sharedSecret = getSharedSecret(incomingPublicKey, keyAgreement);
		log.debug("shared secret: {}", Hex.toHexString(sharedSecret));
			
		byte[] digestOut = calculateHMAC_SHA256(incomingPublicKey, sharedSecret);
		try {
			cipherOut = Cipher.getInstance("RC4");
			cipherOut.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(digestOut, 0, 16, "RC4"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		byte[] digestIn = calculateHMAC_SHA256(outgoingPublicKey, sharedSecret);
		try {
			cipherIn = Cipher.getInstance("RC4");
			cipherIn.init(Cipher.DECRYPT_MODE, new SecretKeySpec(digestIn, 0, 16, "RC4"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		// setting up part 2
		byte[] part2 = new byte[HANDSHAKE_SIZE];
		
		pos = 0;
		
		for(int i=HANDSHAKE_SIZE; i<HANDSHAKE_SIZE*2; i++) {
			part2[pos++] = handledResponse[i];
		}
		
		log.debug("Server response part 2: {}", part2);
		
		// validate server response part 2, not really required for client, but just to show off ;)
		byte[] firstFourBytes = getFourBytesFrom(part2, 0);
		if (Arrays.equals(new byte[] { 0, 0, 0, 0 }, firstFourBytes)) {
			log.warn("Server response part 2 first four bytes are zero, did handshake fail ?");
		}
		
		message = new byte[HANDSHAKE_SIZE - DIGEST_LENGTH];
		
		pos = 0;
		
		for(int i=0; i<message.length; i++) {
			message[i] = part2[pos++];
		}
		
		digest = calculateHMAC_SHA256(outgoingDigest, GENUINE_FMS_KEY);
		
		byte[] signature = calculateHMAC_SHA256(message, digest);
		byte[] serverSignature = new byte[DIGEST_LENGTH];

		for(int i=0; i<serverSignature.length; i++) {
			serverSignature[i] = part2[pos++];
		}
		
		if (Arrays.equals(signature, serverSignature)) {
			log.info("server response part 2 validation success, is Flash Player v9 handshake");
		} else {
			log.warn("server response part 2 validation failed, not Flash Player v9 handshake");
		}
		
		// swf verification
		if (swfHash != null) {
			byte[] bytesFromServer = new byte[DIGEST_LENGTH];
			
			pos = HANDSHAKE_SIZE - DIGEST_LENGTH;
			
			for(int i=0; i<bytesFromServer.length; i++) {
				bytesFromServer[i] = part1[pos++];
			}
			
			byte[] bytesFromServerHash = calculateHMAC_SHA256(swfHash, bytesFromServer);
			// construct SWF verification pong payload
			byte[] swfv = new byte[42];
			swfv[0] = (byte) 0x01;
			swfv[1] = (byte) 0x01;
			
			byte[] byteSwfSize = intToByteArray(swfSize);
			
			int startPos = 2;
			
			for(int i=0; i<byteSwfSize.length; i++) {
				swfv[startPos++] = byteSwfSize[i];
			}
			
			for(int i=0; i<byteSwfSize.length; i++) {
				swfv[startPos++] = byteSwfSize[i];
			}
			
			for(int i=0; i<bytesFromServerHash.length; i++) {
				swfv[startPos++] = bytesFromServerHash[i];
			}
			
			swfVerificationBytes = swfv;
			
			log.info("initialized swf verification response from swfSize: {} & swfHash: {} = {}",
					new Object[] { swfSize, Hex.toHexString(swfHash), Hex.toHexString(swfVerificationBytes) });
		}
		
		return true;
	}
	
	/**
	 * Get first handshake response from the server
	 */
	private byte[] receiveFirstHandshakeRawResponse() throws Exception {

		InputStream is = socket.getInputStream();
		
		int maxAttempts = 100;
		int attempts = 0;
		
		while(is.available() < 3073) {
			if(attempts > maxAttempts) break;
			Thread.sleep(50);
			attempts++;
		}
		
		byte[] buf = new byte[is.available()];
		
		is.read(buf);
		
		log.debug("Handshake size: {}", new Object[] { buf.length });
		
		return buf;
	}
	
	/**
	 * Second handshake request to server
	 */
	private void secondRequest() throws Exception {
		byte[] randomBytes = new byte[HANDSHAKE_SIZE];
		random.nextBytes(randomBytes);
		
		byte[] digest = calculateHMAC_SHA256(incomingDigest, GENUINE_FP_KEY);
		byte[] message = new byte[HANDSHAKE_SIZE - DIGEST_LENGTH];
		
		int pos = 0;
		
		for(int i=0; i<message.length; i++) {
			message[i] = randomBytes[pos++];
		}
		
		byte[] signature = calculateHMAC_SHA256(message, digest);
		
		for(int i=0; i<signature.length; i++) {
			randomBytes[pos++] = signature[i];
		}
		
		if (handshakeType == HandshakeType.ENCRYPTED) {
			// update 'encoder / decoder state' for the RC4 keys. Both parties *pretend* as if handshake part 2 (1536 bytes) was encrypted
			// effectively this hides / discards the first few bytes of encrypted session which is known to increase the secure-ness of RC4
			// RC4 state is just a function of number of bytes processed so far that's why we just run 1536 arbitrary bytes through the keys below
			byte[] dummyBytes = new byte[HANDSHAKE_SIZE];
			cipherIn.update(dummyBytes);
			cipherOut.update(dummyBytes);
		}
		
		socket.getOutputStream().write(randomBytes);
		socket.getOutputStream().flush();
	}
	
	/**
	 * First handshake request to server
	 * @throws Exception Request was crashed...
	 */
	private void firstRequest() throws Exception {
		
		byte[] request = new byte[HANDSHAKE_SIZE + 1];
		request[0] = HandshakeType.NON_ENCRYPTED;
		
		byte[] buffer = new byte[HANDSHAKE_SIZE];
		
		int pos = 0;
		
		for(int i=0; i<handshakeBytes.length; i++) {
			buffer[pos++] = handshakeBytes[i];
		}
		
		// create our keypair
		KeyPair keyPair = generateKeyPair();
		
		outgoingPublicKey = getPublicKey(keyPair);
		
		log.debug("Client public key: {}", Hex.toHexString(outgoingPublicKey));
		
		byte[] dhPointer = getFourBytesFrom(buffer, HANDSHAKE_SIZE - 4);
		int dhOffset = calculateOffset(dhPointer, 632, 772);
		
		pos = 0;
		
		for(int i=dhOffset; i<dhOffset+outgoingPublicKey.length; i++) {
			buffer[i] = outgoingPublicKey[pos++]; 
		}
		
		byte[] digestPointer = getFourBytesFrom(buffer, 8);
		int digestOffset = calculateOffset(digestPointer, 728, 12);
		
		int messageLength = HANDSHAKE_SIZE - DIGEST_LENGTH;
		
		byte[] message = new byte[messageLength];
		
		pos = 0;
		
		for(int i=0; i<digestOffset; i++) {
			message[pos++] = buffer[i];
		}
		
		int afterDigestOffset = digestOffset + DIGEST_LENGTH;
		
		for(int i=afterDigestOffset; i<HANDSHAKE_SIZE; i++) {
			message[pos++] = buffer[i];
		}

		outgoingDigest = calculateHMAC_SHA256(message, CLIENT_CONST);
		
		pos = 0;
		
		for(int i=digestOffset; i<digestOffset+outgoingDigest.length; i++) {
			buffer[i] = outgoingDigest[pos++];
		}
		
		pos = 0;
		
		for(int i=1; i<request.length; i++) {
			request[i] = buffer[pos++];
		}
		
		socket.getOutputStream().write(request);
		socket.getOutputStream().flush();
		
	}

	/**
	 * Calculates an HMAC SHA256 hash using a default key length.
	 * 
	 * @param input
	 * @param key
	 * @return hmac hashed bytes
	 */
	private byte[] calculateHMAC_SHA256(byte[] input, byte[] key) {
		byte[] output = null;
		try {
			hmacSHA256.init(new SecretKeySpec(key, "HmacSHA256"));
			output = hmacSHA256.doFinal(input);
		} catch (InvalidKeyException e) {
			log.error("Invalid key", e);
		}
		return output;
	}
	
	/**
	 * Calculates an HMAC SHA256 hash using a set key length.
	 * 
	 * @param input
	 * @param key
	 * @param length
	 * @return hmac hashed bytes
	 */
	private byte[] calculateHMAC_SHA256(byte[] input, byte[] key, int length) {
		byte[] output = null;
		try {
			hmacSHA256.init(new SecretKeySpec(key, 0, length, "HmacSHA256"));
			output = hmacSHA256.doFinal(input);
		} catch (InvalidKeyException e) {
			log.error("Invalid key", e);
		}
		return output;
	}
	
	/**
	 * Returns the public key for a given key pair.
	 * 
	 * @param keyPair
	 * @return public key
	 */
	private static byte[] getPublicKey(KeyPair keyPair) {
		 DHPublicKey incomingPublicKey = (DHPublicKey) keyPair.getPublic();
	     BigInteger	dhY = incomingPublicKey.getY();
	     log.debug("Public key: {}", dhY);
	     byte[] result = dhY.toByteArray();
	     //log.debug("Public key as bytes - length [{}]: {}", result.length, Hex.encodeHexString(result));
	     byte[] temp = new byte[KEY_LENGTH];
	     if (result.length < KEY_LENGTH) {
	    	 System.arraycopy(result, 0, temp, KEY_LENGTH - result.length, result.length);
	    	 result = temp;
	    	 log.debug("Padded public key length to 128");
	     } else if(result.length > KEY_LENGTH){
	    	 System.arraycopy(result, result.length - KEY_LENGTH, temp, 0, KEY_LENGTH);
	    	 result = temp;
	    	 log.debug("Truncated public key length to 128");
	     }
	     return result;
	}
	
	/**
	 * Determines the validation scheme for given input.
	 * 
	 * @param otherPublicKeyBytes
	 * @param agreement
	 * @return shared secret bytes if client used a supported validation scheme
	 */
	private static byte[] getSharedSecret(byte[] otherPublicKeyBytes, KeyAgreement agreement) {
		BigInteger otherPublicKeyInt = new BigInteger(1, otherPublicKeyBytes);
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("DH");
			KeySpec otherPublicKeySpec = new DHPublicKeySpec(otherPublicKeyInt, DH_MODULUS, DH_BASE);
			PublicKey otherPublicKey = keyFactory.generatePublic(otherPublicKeySpec);
			agreement.doPhase(otherPublicKey, true);
		} catch (Exception e) {
			log.error("Exception getting the shared secret", e);
		}
		byte[] sharedSecret = agreement.generateSecret();
		//log.debug("Shared secret [{}]: {}", sharedSecret.length, Hex.encodeHexString(sharedSecret));
		return sharedSecret;
	}	
	
	private int addBytes(byte[] bytes) {
		if (bytes.length != 4) {
			throw new RuntimeException("Unexpected byte array size: " + bytes.length);
		}
		int result = 0;
		for (int i = 0; i < bytes.length; i++) {
			result += bytes[i] & 0xff;
		}
		return result;
	}
	
	private int calculateOffset(byte[] pointer, int modulus, int increment) {
		int offset = addBytes(pointer);
		offset %= modulus;
		offset += increment;
		return offset;
	}

	private byte[] getFourBytesFrom(byte[] buf, int offset) {
		
		if(offset+4 > buf.length) {
			throw new RuntimeException("Failed offset for input buffer...");
		}
		
		byte[] bytes = new byte[4];
		int pos = 0;
		
		for(int i=offset; i<offset+4; i++) {
			bytes[pos++] = buf[i]; 
		}
		
		return bytes;
	}
	
	public static final byte[] intToByteArray(int value) {
	    return new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
	}
	
	public class HandshakeType {
		public final static int ENCRYPTED = 6;
		public final static int NON_ENCRYPTED = 3;
	}
	
	/**
	 * Initialize SWF verification data.
	 * 
	 * @param swfFilePath path to the swf file or null
	 */
	public void initSwfVerification(String swfFilePath) {
		log.info("Initializing swf verification for: {}", swfFilePath);
		byte[] bytes = null;
		if (swfFilePath != null) {
			File localSwfFile = new File(swfFilePath);
			if (localSwfFile.exists() && localSwfFile.canRead()) {
				log.info("Swf file path: {}", localSwfFile.getAbsolutePath());
				bytes = FileUtil.readAsByteArray(localSwfFile);
			} else {
				bytes = "Rtmplight is awesome for handling non-accessable swf file".getBytes();
			}
		} else {
			bytes = new byte[42];
		}
		swfHash = calculateHMAC_SHA256(bytes, CLIENT_CONST, 30);
		swfSize = bytes.length;
		log.info("Verification - size: {}, hash: {}", swfSize, Hex.toHexString(swfHash));
	}
}
