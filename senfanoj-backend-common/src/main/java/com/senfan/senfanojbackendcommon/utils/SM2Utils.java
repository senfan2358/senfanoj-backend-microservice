package com.senfan.senfanojbackendcommon.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

/**
 * 国密
 */
@Slf4j
public class SM2Utils {
    /**
     * 椭圆曲线ECParameters ASN.1 结构
     */
    private final static X9ECParameters x9ECParameters = GMNamedCurves.getByName("sm2p256v1");
    /**
     * 椭圆曲线公钥或私钥的基本域参数
     */
    private final static ECParameterSpec ecDomainParameters = new ECParameterSpec(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN());

    /**
     * 生成密钥对
     */
    static KeyPair createECKeyPair() {
        // 使用标准名称创建EC参数生成的参数规范
        final ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");

        // 获取一个椭圆曲线类型的密钥对生成器
        final KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            // 使用SM2算法域参数集初始化密钥生成器（默认使用以最高优先级安装的提供者的 SecureRandom 的实现作为随机源）
            // kpg.initialize(sm2Spec);

            // 使用SM2的算法域参数集和指定的随机源初始化密钥生成器
            kpg.initialize(sm2Spec, new SecureRandom());

            // 通过密钥生成器生成密钥对
            return kpg.generateKeyPair();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 公钥加密
     *
     * @param publicKey 公钥
     * @param data      加密数据
     * @param modeType  加密模式
     * @return
     */
    public static String encrypt(String publicKey, String data, int modeType) {
        return encrypt(getECPublicKeyByPublicKeyHex(publicKey), data, modeType);
    }

    /**
     * 公钥加密,默认模式1
     *
     * @param publicKey
     * @param data
     * @return
     */
    public static String encrypt(String publicKey, String data) {
        return encrypt(getECPublicKeyByPublicKeyHex(publicKey), data, 1);
    }

    /**
     * 公钥加密
     *
     * @param publicKey SM2公钥
     * @param data      明文数据
     * @param modeType  加密模式
     * @return
     */
    public static String encrypt(BCECPublicKey publicKey, String data, int modeType) {
        if (StringUtils.isBlank(data)) {
            return null;
        }
        // 加密模式
        SM2Engine.Mode mode;
        if (modeType == 1) {
            mode = SM2Engine.Mode.C1C3C2;
        } else {
            mode = SM2Engine.Mode.C1C2C3;
        }

        // 通过公钥对象获取公钥的基本域参数。
        ECParameterSpec ecParameterSpec = publicKey.getParameters();
        ECDomainParameters ecDomainParameters = new ECDomainParameters(ecParameterSpec.getCurve(),
                ecParameterSpec.getG(), ecParameterSpec.getN());

        // 通过公钥值和公钥基本参数创建公钥参数对象
        ECPublicKeyParameters ecPublicKeyParameters = new ECPublicKeyParameters(publicKey.getQ(), ecDomainParameters);

        // 根据加密模式实例化SM2公钥加密引擎
        SM2Engine sm2Engine = new SM2Engine(mode);

        // 初始化加密引擎
        sm2Engine.init(true, new ParametersWithRandom(ecPublicKeyParameters, new SecureRandom()));

        byte[] arrayOfBytes = null;
        try {
            // 将明文字符串转换为指定编码的字节串
            byte[] in = data.getBytes("utf-8");

            // 通过加密引擎对字节数串行加密
            arrayOfBytes = sm2Engine.processBlock(in, 0, in.length);
        } catch (Exception e) {
            log.info("SM2加密时出现异常:{}", e.getMessage());
            e.printStackTrace();
        }

        // 将加密后的字节串转换为十六进制字符串
        return Hex.toHexString(arrayOfBytes);
    }

    /**
     * 私钥解密
     *
     * @param privateKey 私钥
     * @param cipherData 密文数据
     * @param modeType
     * @return
     */
    public static String decrypt(String privateKey, String cipherData, int modeType) {
        return decrypt(getBCECPrivateKeyByPrivateKeyHex(privateKey), cipherData, modeType);
    }

    /**
     * 私钥解密，默认模式1
     *
     * @param privateKey
     * @param cipherData
     * @return
     */
    public static String decrypt(String privateKey, String cipherData) {
        return decrypt(getBCECPrivateKeyByPrivateKeyHex(privateKey), cipherData, 1);
    }

    /**
     * 私钥解密
     *
     * @param privateKey SM私钥
     * @param cipherData 密文数据
     * @return
     */
    public static String decrypt(BCECPrivateKey privateKey, String cipherData, int modeType) {
        if (StringUtils.isBlank(cipherData)) {
            return null;
        }
        // 解密模式
        SM2Engine.Mode mode;
        if (modeType == 1) {
            mode = SM2Engine.Mode.C1C3C2;
        } else {
            mode = SM2Engine.Mode.C1C2C3;
        }

        // 将十六进制字符串密文转换为字节数组（需要与加密一致，加密是：加密后的字节数组转换为了十六进制字符串）
        byte[] cipherDataByte = Hex.decode(cipherData);

        // 通过私钥对象获取私钥的基本域参数。
        ECParameterSpec ecParameterSpec = privateKey.getParameters();
        ECDomainParameters ecDomainParameters = new ECDomainParameters(ecParameterSpec.getCurve(),
                ecParameterSpec.getG(), ecParameterSpec.getN());

        // 通过私钥值和私钥钥基本参数创建私钥参数对象
        ECPrivateKeyParameters ecPrivateKeyParameters = new ECPrivateKeyParameters(privateKey.getD(),
                ecDomainParameters);

        // 通过解密模式创建解密引擎并初始化
        SM2Engine sm2Engine = new SM2Engine(mode);
        sm2Engine.init(false, ecPrivateKeyParameters);

        String result = null;
        try {
            // 通过解密引擎对密文字节串进行解密
            byte[] arrayOfBytes = sm2Engine.processBlock(cipherDataByte, 0, cipherDataByte.length);
            // 将解密后的字节串转换为utf8字符编码的字符串（需要与明文加密时字符串转换成字节串所指定的字符编码保持一致）
            result = new String(arrayOfBytes, "utf-8");
        } catch (Exception e) {
            log.info("SM2解密时出现异常{}", e.getMessage());
        }
        return result;
    }

    /**
     * 公钥字符串转换为 BCECPublicKey 公钥对象
     *
     * @param pubKeyHex 64字节十六进制公钥字符串(如果公钥字符串为65字节首个字节为0x04：表示该公钥为非压缩格式，操作时需要删除)
     * @return BCECPublicKey SM2公钥对象
     */
    public static BCECPublicKey getECPublicKeyByPublicKeyHex(String pubKeyHex) {
        // 截取64字节有效的SM2公钥（如果公钥首个字节为0x04）
        if (pubKeyHex.length() > 128) {
            pubKeyHex = pubKeyHex.substring(pubKeyHex.length() - 128);
        }
        // 将公钥拆分为x,y分量（各32字节）
        String stringX = pubKeyHex.substring(0, 64);
        String stringY = pubKeyHex.substring(stringX.length());
        // 将公钥x、y分量转换为BigInteger类型
        BigInteger x = new BigInteger(stringX, 16);
        BigInteger y = new BigInteger(stringY, 16);
        // 通过公钥x、y分量创建椭圆曲线公钥规范
        ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(x9ECParameters.getCurve().createPoint(x, y), ecDomainParameters);
        // 通过椭圆曲线公钥规范，创建出椭圆曲线公钥对象（可用于SM2加密及验签）
        return new BCECPublicKey("EC", ecPublicKeySpec, BouncyCastleProvider.CONFIGURATION);
    }


    /**
     * 私钥字符串转换为 BCECPrivateKey 私钥对象
     *
     * @param privateKeyHex: 32字节十六进制私钥字符串
     * @return BCECPrivateKey:SM2私钥对象
     */
    public static BCECPrivateKey getBCECPrivateKeyByPrivateKeyHex(String privateKeyHex) {
        // 将十六进制私钥字符串转换为BigInteger对象
        BigInteger d = new BigInteger(privateKeyHex, 16);
        // 通过私钥和私钥域参数集创建椭圆曲线私钥规范
        ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(d, ecDomainParameters);
        // 通过椭圆曲线私钥规范，创建出椭圆曲线私钥对象（可用于SM2解密和签名）
        return new BCECPrivateKey("EC", ecPrivateKeySpec, BouncyCastleProvider.CONFIGURATION);
    }

    /**
     * 国密验证
     *
     * @param privateKey
     * @param content
     * @param sign
     * @param modeType
     * @return
     */
    public static boolean verify(String privateKey, String content, String sign, int modeType) {
        String encryptContent = decrypt(privateKey, sign, modeType);
        if (!content.equals(encryptContent)) {
            return false;
        }
        return true;
    }

    /**
     * 国密验证，默认模式1
     *
     * @param privateKey
     * @param content
     * @param sign
     * @return
     */
    public static boolean verify(String privateKey, String content, String sign) {
        if (StringUtils.isBlank(content) && StringUtils.isBlank(sign)){
            return true;
        }
        if (StringUtils.isAnyBlank(content,sign)){
            return false;
        }
        String encryptContent = decrypt(privateKey, sign, 1);
        if (!content.equals(encryptContent)) {
            return false;
        }
        return true;
    }

    /**
     * 获取公私钥
     *
     * @return String[] 返回公私钥
     */
    public static String[] getKeys() {
        KeyPair ecKeyPair = createECKeyPair();
        PrivateKey privateKey = ecKeyPair.getPrivate();
        PublicKey publicKey = ecKeyPair.getPublic();
        String publicHex = Hex.toHexString(((BCECPublicKey) publicKey).getQ().getEncoded(false));
        String privateHex = ((BCECPrivateKey) privateKey).getD().toString(16);
        return new String[]{publicHex, privateHex};
    }

    public static void main(String[] args) {
        // String[] keys = getKeys();
        // String publicKey = keys[0];
        // String privateKey = keys[1];
        // System.out.println(publicKey);
        // System.out.println(privateKey);
        // String sign = encrypt("043e9e993d5e0eb9f9a92808c88d9ab657e19560f394ff67b68f40f18c47165c170dacb937c08e042bf8e5d1302ce28bedd51c0925c1f9993b585044d6b43bf686", "");
        // String decrypt = decrypt("9ede4b055debfbeeb88bf1113ea05a19c8a2123653a39b0a54cb116d5dd281c6", sign);
        // boolean res = verify("9ede4b055debfbeeb88bf1113ea05a19c8a2123653a39b0a54cb116d5dd281c6", "", sign);
        // System.out.println(res);
        // System.out.println(decrypt);
        // String temp = encrypt(publicKey, "senfan");
        // String resTemp = decrypt(privateKey, temp);
        // boolean senfan = verify(privateKey, "senfan", temp);
        // System.out.println(resTemp);
        // System.out.println(senfan);
        String sign = encrypt("04e8bdc14567991868c120841cfe9b9394e571abd11920888e686445f8d5480c538574b2d9d72010526441f11c4ec7c8d1b8ba8bd6a45f391a043a88b32af4aad0", "senfan235");
        boolean verify = verify("84790929baa920b10555c4095764476229114cf89cf2d344778710286c9f9195", "senfan235", sign);
        System.out.println(verify);
    }
}
