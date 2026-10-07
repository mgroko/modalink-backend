package org.mgroko.backend.storage.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Borra archivos del almacenamiento sincronizados con la transacci&oacute;n en curso:
 * despu&eacute;s del commit se borra el archivo viejo y, si la transacci&oacute;n termina
 * sin commit, se borra el reci&eacute;n escrito.
 * <p>
 * Un fallo al borrar nunca se propaga: se registra con SLF4J y el archivo queda
 * para el job de limpieza ({@link LimpiezaImagenesService}).
 */
@Component
public class BorradoArchivosDiferido {

    private static final Logger LOG = LoggerFactory.getLogger(BorradoArchivosDiferido.class);

    private final StorageService storageService;

    public BorradoArchivosDiferido(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Deja programados los borrados de archivo sincronizados con la transacci&oacute;n.
     *
     * @param archivoNuevo    archivo reci&eacute;n escrito que se borra si no hay commit, o {@code null}
     * @param archivoAnterior archivo a borrar despu&eacute;s del commit, o {@code null}
     */
    public void registrarBorrados(String archivoNuevo, String archivoAnterior) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Sin transacci&oacute;n no hay commit que esperar: la escritura de la BD ya es efectiva
            if (archivoAnterior != null) {
                borrar(archivoAnterior, "sin transacci\u00f3n activa");
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (archivoAnterior != null) {
                    borrar(archivoAnterior, "despu\u00e9s del commit");
                }
            }

            @Override
            public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED && archivoNuevo != null) {
                    borrar(archivoNuevo, "transacci\u00f3n sin commit");
                }
            }
        });
    }

    /**
     * Borra un archivo del almacenamiento sin propagar el fallo: se registra la
     * ruta y la excepci&oacute;n para que el job de limpieza pueda reintentarlo.
     *
     * @param nombreArchivo nombre del archivo a borrar
     * @param motivo        raz&oacute;n del borrado, para el registro
     */
    public void borrar(String nombreArchivo, String motivo) {
        try {
            storageService.eliminarFotoPerfil(nombreArchivo);
        } catch (Exception e) {
            LOG.warn("No se pudo eliminar el archivo '{}' ({}); queda para el job de limpieza.",
                    nombreArchivo, motivo, e);
        }
    }
}
