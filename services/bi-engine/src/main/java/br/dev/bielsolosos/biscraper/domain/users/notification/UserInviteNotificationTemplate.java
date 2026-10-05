package br.dev.bielsolosos.biscraper.domain.users.notification;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.utils.NotificationTemplate;
import lombok.Getter;

@Getter
public class UserInviteNotificationTemplate implements NotificationTemplate {

    private final String recipientEmail;
    private final String inviteUrl;
    private final int expirationHours;
    private final String invitedByName;

    public UserInviteNotificationTemplate(String recipientEmail, String inviteUrl, int expirationHours, String invitedByName) {
        this.recipientEmail = recipientEmail;
        this.inviteUrl = inviteUrl;
        this.expirationHours = expirationHours > 0 ? expirationHours : 48;
        this.invitedByName = (invitedByName != null && !invitedByName.isBlank()) ? invitedByName : "Administrador";
    }

    @Override
    public NotificationChannel[] getChannels() {
        return new NotificationChannel[]{NotificationChannel.EMAIL};
    }

    @Override
    public String getSubject() {
        return "Você foi convidado para o Bobão do Oeste!";
    }

    @Override
    public String getMessageTemplate() {
        return String.format(
                "Olá!\n\nVocê foi convidado por %s para fazer parte da plataforma Bobão do Oeste.\n\n"
                        + "Para aceitar o convite e criar sua conta, acesse o link:\n%s\n\n"
                        + "Este convite é válido por %d horas. Se você não esperava por este convite, pode ignorar esta mensagem.",
                invitedByName,
                inviteUrl,
                expirationHours
        );
    }

    @Override
    public String toHtmlEmail() {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Convite de Acesso</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #0b0f19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b;">
                  <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #0b0f19; padding: 40px 10px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 540px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.3);">
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
                              <h2 style="margin: 0 0 16px 0; color: #111827; font-size: 20px; font-weight: 600;">Você recebeu um convite!</h2>
                              <p style="margin: 0 0 20px 0; color: #4b5563; font-size: 15px; line-height: 1.6;">
                                Olá! <strong>%s</strong> convidou você para criar uma conta na plataforma <strong>Bobão do Oeste</strong>.
                              </p>
                              <p style="margin: 0 0 28px 0; color: #4b5563; font-size: 15px; line-height: 1.6;">
                                Clique no botão abaixo para definir seu nome de usuário e senha e começar a rastrear oportunidades em marketplaces:
                              </p>

                              <!-- CTA Button -->
                              <div style="text-align: center; margin: 32px 0;">
                                <a href="%s" style="background-color: #f59e0b; color: #0b0f19; font-weight: 700; font-size: 15px; text-decoration: none; padding: 14px 32px; border-radius: 10px; display: inline-block; box-shadow: 0 4px 12px rgba(245, 158, 11, 0.35);">
                                  Aceitar Convite e Criar Conta
                                </a>
                              </div>

                              <p style="margin: 24px 0 0 0; color: #6b7280; font-size: 12px; line-height: 1.5; text-align: center;">
                                Se o botão acima não funcionar, copie e cole o link abaixo em seu navegador:<br>
                                <a href="%s" style="color: #d97706; word-break: break-all; font-size: 11px;">%s</a>
                              </p>

                              <hr style="border: 0; border-top: 1px solid #f3f4f6; margin: 28px 0 20px 0;">

                              <p style="margin: 0; color: #9ca3af; font-size: 12px; line-height: 1.4;">
                                &bull; Este convite expira em <strong>%d horas</strong>.<br>
                                &bull; Se você não esperava este convite, desconsidere esta mensagem.
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="background-color: #f9fafb; padding: 20px 40px; text-align: center; border-top: 1px solid #e5e7eb;">
                              <p style="margin: 0; color: #9ca3af; font-size: 12px;">
                                &copy; %d Bobão do Oeste &bull; Convite oficial de acesso
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                invitedByName,
                inviteUrl,
                inviteUrl,
                inviteUrl,
                expirationHours,
                java.time.Year.now().getValue()
        );
    }
}
