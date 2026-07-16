package com.example.data

import android.util.Log
import com.example.core.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

sealed interface SmtpResult {
    object Success : SmtpResult
    data class AuthFailure(val message: String) : SmtpResult
    data class NetworkError(val message: String) : SmtpResult
    data class GenericFailure(val message: String) : SmtpResult
}

object GmailSmtpSender {
    private const val TAG = "GmailSmtpSender"

    val smtpUser: String
        get() = try { BuildConfig.SMTP_GMAIL_USER } catch (e: Exception) { "" }

    val smtpPassword: String
        get() = try { BuildConfig.SMTP_GMAIL_APP_PASS } catch (e: Exception) { "" }

    /**
     * Checks whether Gmail SMTP credentials are configured and valid
     */
    fun isConfigured(): Boolean {
        val user = smtpUser
        val pass = smtpPassword
        return user.isNotBlank() && 
               !user.contains("your_email") && 
               !user.contains("SMTP_GMAIL_USER_PLACEHOLDER") &&
               pass.isNotBlank() && 
               !pass.contains("your_app_password") &&
               !pass.contains("SMTP_GMAIL_APP_PASS_PLACEHOLDER")
    }

    /**
     * Sends an email securely via Gmail SMTP on port 587 with STARTTLS
     */
    suspend fun sendEmail(
        recipient: String,
        subject: String,
        bodyHtml: String
    ): SmtpResult = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            Log.w(TAG, "Gmail SMTP is not fully configured in Secrets Panel. Falling back to simulation.")
            return@withContext SmtpResult.GenericFailure("SMTP is not configured in Secrets Panel.")
        }

        try {
            val props = Properties().apply {
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
                // Reduced timeouts (3s) to prevent the app from lagging when ports are blocked by cloud firewalls
                put("mail.smtp.connectiontimeout", "3000")
                put("mail.smtp.timeout", "3000")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(smtpUser, smtpPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpUser, "SwapnoFlix Streaming Platform"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient))
                setSubject(subject, "UTF-8")
                setContent(bodyHtml, "text/html; charset=utf-8")
            }

            Transport.send(message)
            Log.i(TAG, "Success! Real email dispatched successfully via Gmail SMTP to $recipient")
            SmtpResult.Success
        } catch (e: javax.mail.AuthenticationFailedException) {
            Log.e(TAG, "SMTP Authentication Failed for $recipient: ${e.localizedMessage}", e)
            SmtpResult.AuthFailure(e.localizedMessage ?: "Invalid Gmail SMTP credentials.")
        } catch (e: com.sun.mail.util.MailConnectException) {
            Log.e(TAG, "SMTP Connection Failed (likely outbound port 587 blocked) for $recipient: ${e.localizedMessage}", e)
            SmtpResult.NetworkError(e.localizedMessage ?: "Connection timed out.")
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("timeout", ignoreCase = true) || msg.contains("connect", ignoreCase = true) || e is java.io.IOException) {
                Log.e(TAG, "SMTP Connection/Timeout Error for $recipient: $msg", e)
                SmtpResult.NetworkError(msg)
            } else {
                Log.e(TAG, "SMTP Delivery Failed to $recipient: $msg", e)
                SmtpResult.GenericFailure(msg)
            }
        }
    }
}
