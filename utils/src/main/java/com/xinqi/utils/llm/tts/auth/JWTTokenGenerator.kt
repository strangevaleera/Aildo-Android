package com.xinqi.utils.llm.tts.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

/**
 * JWT Token生成器
 * 用于生成API的鉴权token
 * 参考文档: https://www.sensecore.cn/help/docs/model-as-a-service/nova/overview/Authorization
 */
object JWTTokenGenerator {
    
    /**
     * 生成JWT token
     * @param ak Access Key ID
     * @param sk Access Key Secret
     * @param expirationTime 过期时间（秒），默认30分钟
     * @param notBeforeTime 生效时间（秒），默认当前时间-5秒
     * @return JWT token字符串
     */
    fun generateToken(
        ak: String,
        sk: String,
        expirationTime: Long = 1800L, // 30分钟
        notBeforeTime: Long = -5L // 当前时间-5秒
    ): String {
        val currentTime = System.currentTimeMillis() / 1000
        
        return try {
            val algorithm = Algorithm.HMAC256(sk)
            val header = mapOf(
                "alg" to "HS256",
                "typ" to "JWT"
            )
            
            JWT.create()
                .withHeader(header)
                .withIssuer(ak)
                .withExpiresAt(Date((currentTime + expirationTime) * 1000))
                .withNotBefore(Date((currentTime + notBeforeTime) * 1000))
                .sign(algorithm)
                
        } catch (e: Exception) {
            throw RuntimeException("生成JWT token失败: ${e.message}", e)
        }
    }
    
    /**
     * 验证token是否即将过期
     * @param token JWT token
     * @param thresholdSeconds 阈值时间（秒），默认5分钟
     * @return 是否需要刷新token
     */
    fun isTokenExpiringSoon(token: String, thresholdSeconds: Long = 300L): Boolean {
        return try {
            val decodedJWT = JWT.decode(token)
            val expirationTime = decodedJWT.expiresAt?.time ?: 0
            val currentTime = System.currentTimeMillis()
            val timeUntilExpiration = expirationTime - currentTime
            
            timeUntilExpiration <= thresholdSeconds * 1000
        } catch (e: Exception) {
            true
        }
    }
}
