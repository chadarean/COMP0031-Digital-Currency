package com.mycompany.app;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.RSABlindingEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSABlindingFactorGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSABlindingParameters;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.signers.PSSSigner;


public class BlindSignature {

    public static AsymmetricCipherKeyPair generateKeys(int keySize) {
        RSAKeyPairGenerator r = new RSAKeyPairGenerator();
        r.init(new RSAKeyGenerationParameters(new BigInteger("10001", 16), new SecureRandom(),keySize, 80));
        AsymmetricCipherKeyPair keys = r.generateKeyPair();
        return keys;
    }

    public static BigInteger generateBlindingFactor(CipherParameters pubKey) {
        RSABlindingFactorGenerator gen = new RSABlindingFactorGenerator();
        gen.init(pubKey);
        return gen.generateBlindingFactor();
    }

    public static byte[] blind(CipherParameters key, BigInteger factor, byte[] msg) {
        RSABlindingEngine engine = new RSABlindingEngine();
        RSABlindingParameters params = new RSABlindingParameters((RSAKeyParameters) key, factor);
        PSSSigner blindSigner = new PSSSigner(engine, new SHA1Digest(), 15);
        blindSigner.init(true, params);
        blindSigner.update(msg, 0, msg.length);
        byte[] blinded = null;
        try {
            blinded = blindSigner.generateSignature();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return blinded;
    }
    
    public static byte[] unblind(CipherParameters key, BigInteger factor, byte[] msg) {
        RSABlindingEngine engine = new RSABlindingEngine();

        RSABlindingParameters params = new RSABlindingParameters((RSAKeyParameters) key,factor);
        engine.init(false, params);

        return engine.processBlock(msg, 0, msg.length);
    }

    public static byte[] sign(CipherParameters key, byte[] toSign) {
        SHA1Digest digest = new SHA1Digest();
        RSAEngine engine = new RSAEngine();

        PSSSigner signer = new PSSSigner(engine, digest, 15);
        signer.init(true, key);
        signer.update(toSign, 0, toSign.length);

        byte[] sig = null;

        try {
            sig = signer.generateSignature();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return sig;
    }

    public static boolean verify(CipherParameters key, byte[] msg, byte[] sig) {
        PSSSigner signer = new PSSSigner(new RSAEngine(), new SHA1Digest(), 15);
        signer.init(false, key);

        signer.update(msg,0,msg.length);

        return signer.verifySignature(sig);
    }

    public static byte[] signBlinded(CipherParameters key, byte[] msg) {
        // This could be used by MSBs to sign the Alice blinded message
        RSAEngine signer = new RSAEngine();
        signer.init(true, key);
        return signer.processBlock(msg, 0, msg.length);
    }

}
