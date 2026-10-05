package br.dev.bielsolosos.biscraper.domain.users.notification;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.utils.NotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import lombok.Getter;

@Getter
public class EmailOtpNotificationTemplate implements NotificationTemplate {

    private final User recipient;
    private final String otpCode;
    private final int expirationMinutes;

    public EmailOtpNotificationTemplate(User recipient, String otpCode, int expirationMinutes) {
        this.recipient = recipient;
        this.otpCode = otpCode;
        this.expirationMinutes = expirationMinutes > 0 ? expirationMinutes : 10;
    }

    @Override
    public NotificationChannel[] getChannels() {
        return new NotificationChannel[]{NotificationChannel.EMAIL};
    }

    @Override
    public String getSubject() {
        return "Seu código de acesso: " + otpCode + " - Bobão do Oeste";
    }

    @Override
    public String getMessageTemplate() {
        return String.format(
                "Olá, %s!\n\nSeu código de verificação para acesso ao Bobão do Oeste é: %s\n\n"
                        + "Este código expira em %d minutos. Se você não solicitou este acesso, ignore esta mensagem.",
                recipient != null && recipient.getUsername() != null ? recipient.getUsername() : "usuário",
                otpCode,
                expirationMinutes
        );
    }

    @Override
    public String toHtmlEmail() {
        String displayName = recipient != null && recipient.getUsername() != null && !recipient.getUsername().isBlank()
                ? recipient.getUsername()
                : "Usuário";

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Código de Acesso</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #0b0f19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b;">
                  <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #0b0f19; padding: 40px 10px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 520px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.3);">
                          <!-- Header -->
                          <tr>
                            <td style="background-color: #111827; padding: 32px 40px; text-align: center; border-bottom: 2px solid #f59e0b;">
                              <h1 style="margin: 0; color: #ffffff; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;">Bobão do Oeste</h1>
                              <p style="margin: 6px 0 0 0; color: #9ca3af; font-size: 13px;">Marketplace Intelligence & Tracking</p>
                            </td>
                          </tr>
                          <!-- Body Content -->
                          <tr>
                            <td style="padding: 40px 40px 32px 40px;">
                              <h2 style="margin: 0 0 16px 0; color: #111827; font-size: 20px; font-weight: 600;">Código de Verificação</h2>
                              <p style="margin: 0 0 24px 0; color: #4b5563; font-size: 15px; line-height: 1.5;">
                                Olá, <strong>%s</strong>! Você solicitou o acesso à sua conta por código de verificação. Utilize o código de uso único abaixo para prosseguir:
                              </p>

                              <!-- OTP Highlight Box -->
                              <div style="background-color: #f8fafc; border: 2px dashed #f59e0b; border-radius: 12px; padding: 24px; text-align: center; margin: 28px 0;">
                                <div style="font-family: 'Courier New', Courier, monospace; font-size: 38px; font-weight: 800; letter-spacing: 10px; color: #b45309;">
                                  %s
                                </div>
                                <p style="margin: 10px 0 0 0; color: #6b7280; font-size: 12px; font-weight: 500;">
                                  Válido por <strong>%d minutos</strong>
                                </p>
                              </div>

                              <p style="margin: 0 0 12px 0; color: #6b7280; font-size: 13px; line-height: 1.5;">
                                Se você não tentou entrar na plataforma, recomendamos alterar sua senha ou ignorar esta mensagem. Nunca compartilhe este código com ninguém.
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="background-color: #f9fafb; padding: 20px 40px; text-align: center; border-top: 1px solid #e5e7eb;">
                              <p style="margin: 0; color: #9ca3af; font-size: 12px;">
                                &copy; %d Bobão do Oeste &bull; Mensagem automática de segurança
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(displayName, otpCode, expirationMinutes, java.time.Year.now().getValue());
    }
}
