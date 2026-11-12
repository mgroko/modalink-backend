package org.mgroko.backend.notificaciones.email;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.mgroko.backend.modelo.Notificacion;
import org.mgroko.backend.notificaciones.mapper.NotificacionTitulos;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Construye el asunto, el HTML y el texto plano de una notificación.
 *
 * <p>El link apunta al frontend con el token en el fragmento de la URL. El
 * fragmento no se envía al servidor, así que el token no queda en logs de
 * acceso.</p>
 */
@Component
public class NotificacionEmailBuilder {

    private static final String PLANTILLA_HTML = """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>{{titulo}}</title>
            </head>
            <body style="margin:0;padding:0;background:#f5f3f7;font-family:Arial,Helvetica,sans-serif;color:#232134;">
              <span style="display:none;visibility:hidden;opacity:0;color:transparent;height:0;width:0;">
                {{preheader}}
              </span>
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#f5f3f7;padding:24px 12px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;">
                      <tr>
                        <td style="padding:24px 28px;background:#232134;color:#ffffff;">
                          <div style="font-size:20px;font-weight:700;letter-spacing:0.2px;">ModaLink</div>
                          <div style="font-size:12px;opacity:0.75;margin-top:4px;">Networking para la industria creativa y de la moda</div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:28px;">
                          <h1 style="margin:0 0 12px;font-size:22px;line-height:1.25;color:#232134;">{{titulo}}</h1>
                          <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#4b475a;">{{mensaje}}</p>
                          <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 0 20px;">
                            <tr>
                              <td style="border-radius:8px;background:#6c4fd3;">
                                <a href="{{link}}" style="display:inline-block;padding:13px 22px;color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;">
                                  Ver en ModaLink
                                </a>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:0 0 8px;font-size:12px;line-height:1.5;color:#777286;">
                            Si el botón no funciona, copiá y pegá este enlace en tu navegador:
                          </p>
                          <p style="margin:0 0 20px;font-size:12px;line-height:1.5;word-break:break-all;">
                            <a href="{{link}}" style="color:#6c4fd3;">{{link}}</a>
                          </p>
                          <p style="margin:0;font-size:12px;line-height:1.5;color:#777286;">
                            Ingresando como <strong>{{destinatario}}</strong>.
                            Si no reconocés esta cuenta, ignorá este correo.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:16px 28px;background:#faf9fc;border-top:1px solid #ece9f2;font-size:11px;line-height:1.5;color:#8a8599;">
                          Este correo se generó automáticamente. No respondas a este mensaje.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;

    private static final String PLANTILLA_TEXTO = """
            ModaLink

            {{titulo}}

            {{mensaje}}

            Ver en ModaLink:
            {{link}}

            Ingresando como {{destinatario}}.
            Si no reconocés esta cuenta, ignorá este correo.

            Este correo se generó automáticamente. No respondas a este mensaje.
            """;

    private final NotificacionLinkResolver linkResolver;

    public NotificacionEmailBuilder(NotificacionLinkResolver linkResolver) {
        this.linkResolver = linkResolver;
    }

    public EmailContent construir(
            Notificacion notificacion,
            Long idProyecto,
            String tokenCrudo,
            String frontendUrl,
            String destinatario) {

        String titulo = NotificacionTitulos.tituloDe(notificacion.getTipo());
        if (titulo == null || titulo.isBlank()) {
            titulo = notificacion.getTipo() == null
                    ? "Notificación de ModaLink"
                    : notificacion.getTipo().replace('_', ' ');
        }

        String mensaje = notificacion.getMensaje() == null ? "" : notificacion.getMensaje();
        String ruta = linkResolver.resolver(notificacion, idProyecto);
        String link = construirLink(frontendUrl, tokenCrudo, ruta);
        String preheader = recortar(mensaje, 90);

        String html = PLANTILLA_HTML
                .replace("{{link}}", HtmlUtils.htmlEscape(link))
                .replace("{{titulo}}", HtmlUtils.htmlEscape(titulo))
                .replace("{{preheader}}", HtmlUtils.htmlEscape(preheader))
                .replace("{{mensaje}}", HtmlUtils.htmlEscape(mensaje))
                .replace("{{destinatario}}", HtmlUtils.htmlEscape(destinatario == null ? "" : destinatario));

        String texto = PLANTILLA_TEXTO
                .replace("{{link}}", link)
                .replace("{{titulo}}", titulo)
                .replace("{{mensaje}}", mensaje)
                .replace("{{destinatario}}", destinatario == null ? "" : destinatario);

        return new EmailContent(
                destinatario,
                "[ModaLink] " + titulo,
                html,
                texto);
    }

    private String construirLink(String frontendUrl, String tokenCrudo, String ruta) {
        String base = frontendUrl == null || frontendUrl.isBlank()
                ? "http://localhost:5173"
                : frontendUrl.replaceAll("/+$", "");
        String destino = ruta == null || ruta.isBlank()
                ? NotificacionLinkResolver.FALLBACK
                : ruta;
        return base + "/auth/email#token="
                + URLEncoder.encode(tokenCrudo, StandardCharsets.UTF_8)
                + "&redirect="
                + URLEncoder.encode(destino, StandardCharsets.UTF_8);
    }

    private String recortar(String valor, int maximo) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.replaceAll("\\s+", " ").trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }

    public record EmailContent(
            String destinatario,
            String asunto,
            String html,
            String texto) {
    }
}
