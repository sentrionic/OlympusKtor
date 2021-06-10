package xyz.olympusblog.utils

import com.typesafe.config.ConfigFactory
import io.ktor.config.HoconApplicationConfig
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder


object Email {

    private val appConfig = HoconApplicationConfig(ConfigFactory.load())
    private val user = appConfig.property("ktor.gmail.user").getString()
    private val password = appConfig.property("ktor.gmail.password").getString()

    private val mailer = MailerBuilder
        .withSMTPServer("smtp.gmail.com", 587, user, password)
        .withTransportStrategy(TransportStrategy.SMTP_TLS)
        .buildMailer()

    fun sendEmail(email: String, html: String) {
        val mail = EmailBuilder.startingBlank()
            .from("OlympusBlog", user)
            .to(email, email)
            .withSubject("Reset Password")
            .withHTMLText(html)
            .buildEmail()

        mailer.sendMail(mail)
    }

}