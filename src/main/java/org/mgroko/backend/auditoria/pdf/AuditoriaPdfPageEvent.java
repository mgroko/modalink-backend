package org.mgroko.backend.auditoria.pdf;

import java.time.LocalDateTime;

import org.mgroko.backend.common.pdf.ModaLinkPdfPageEvent;

/**
 * Barra superior y pie de página del informe de auditoría.
 *
 * <p>Reutiliza {@link ModaLinkPdfPageEvent} para que la identidad visual y el
 * mecanismo de numeración de hojas sean exactamente los mismos que usa el
 * resto de los PDF de ModaLink.</p>
 */
public class AuditoriaPdfPageEvent extends ModaLinkPdfPageEvent {

    public AuditoriaPdfPageEvent(LocalDateTime impresoEn) {
        super(impresoEn, "ModaLink · Informe de auditoría");
    }
}
