import nodemailer from 'nodemailer';

export interface Env {
  AUTH_SECRET?: string;
  GMAIL_USER?: string;
  GMAIL_APP_PASSWORD?: string;
  FROM_NAME?: string;
}

export interface SendEmailPayload {
  to: string;
  subject: string;
  html?: string;
  text?: string;
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    // Health check endpoint
    if (request.method === 'GET' && (url.pathname === '/health' || url.pathname === '/')) {
      return Response.json({
        status: 'UP',
        service: 'bi-scraper-email-worker',
        timestamp: new Date().toISOString(),
      });
    }

    // Email dispatch endpoint
    if (request.method === 'POST' && (url.pathname === '/send-email' || url.pathname === '/api/v1/email/send')) {
      // 1. Validar autenticação
      const authToken = request.headers.get('x-auth-token') ||
        request.headers.get('Authorization')?.replace(/^Bearer\s+/i, '');
      const expectedSecret = env.AUTH_SECRET || 'bi-scraper-email-secret-token';

      if (!authToken || authToken !== expectedSecret) {
        return Response.json({ error: 'Unauthorized: Invalid or missing authentication token' }, { status: 401 });
      }

      // 2. Parse do payload
      let body: SendEmailPayload;
      try {
        body = await request.json<SendEmailPayload>();
      } catch {
        return Response.json({ error: 'Bad Request: Invalid JSON body' }, { status: 400 });
      }

      if (!body.to || !body.subject || (!body.html && !body.text)) {
        return Response.json({
          error: 'Bad Request: Missing required fields (to, subject, and at least html or text)',
        }, { status: 400 });
      }

      // 3. Credenciais do Gmail
      const user = env.GMAIL_USER || 'gmail-teste-aqui@gmail.com';
      const pass = env.GMAIL_APP_PASSWORD || 'quase vazei meu app gmail';
      const fromName = env.FROM_NAME || 'BI Scraper Notificações';

      try {
        const transporter = nodemailer.createTransport({
          host: 'smtp.gmail.com',
          port: 465,
          secure: true,
          auth: {
            user,
            pass,
          },
        });

        const info = await transporter.sendMail({
          from: `"${fromName}" <${user}>`,
          to: body.to,
          subject: body.subject,
          html: body.html,
          text: body.text,
        });

        console.info(`[EmailWorker] E-mail enviado com sucesso para ${body.to} | MessageId: ${info.messageId}`);

        return Response.json({
          success: true,
          messageId: info.messageId,
          to: body.to,
          subject: body.subject,
        });
      } catch (err: any) {
        console.error('[EmailWorker] Erro ao enviar e-mail via SMTP Gmail:', err);
        return Response.json({
          success: false,
          error: err?.message || 'Failed to send email',
        }, { status: 500 });
      }
    }

    return Response.json({ error: 'Not Found' }, { status: 404 });
  },
};
