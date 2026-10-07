package org.mgroko.backend.storage.servicio;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mgroko.backend.storage.exception.ErrorAlmacenamientoException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Los borrados se sincronizan con la transacci&oacute;n: se ejecutan tras el
 * commit (archivo anterior) o cuando no hay commit (archivo nuevo); sin
 * transacci&oacute;n el borrado es inmediato y un fallo nunca se propaga.
 */
class BorradoArchivosDiferidoTest {

    private StorageService storageService;
    private BorradoArchivosDiferido borradoArchivosDiferido;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        borradoArchivosDiferido = new BorradoArchivosDiferido(storageService);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sinTransaccion_borraElArchivoAnteriorDeInmediato() {
        borradoArchivosDiferido.registrarBorrados("nuevo.webp", "anterior.webp");

        verify(storageService).eliminarFotoPerfil("anterior.webp");
        verify(storageService, never()).eliminarFotoPerfil("nuevo.webp");
    }

    @Test
    void sinTransaccion_sinArchivos_noBorraNada() {
        borradoArchivosDiferido.registrarBorrados(null, null);

        verify(storageService, never()).eliminarFotoPerfil("nuevo.webp");
        verify(storageService, never()).eliminarFotoPerfil("anterior.webp");
    }

    @Test
    void conTransaccion_noBorraNadaAntesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();

        borradoArchivosDiferido.registrarBorrados("nuevo.webp", "anterior.webp");

        verify(storageService, never()).eliminarFotoPerfil("nuevo.webp");
        verify(storageService, never()).eliminarFotoPerfil("anterior.webp");
        assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
    }

    @Test
    void conTransaccion_borraElAnteriorTrasElCommit_yConservaElNuevo() {
        TransactionSynchronizationManager.initSynchronization();
        borradoArchivosDiferido.registrarBorrados("nuevo.webp", "anterior.webp");
        TransactionSynchronization sincronizacion = unicaSincronizacion();

        sincronizacion.afterCommit();

        verify(storageService).eliminarFotoPerfil("anterior.webp");

        sincronizacion.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        verify(storageService, never()).eliminarFotoPerfil("nuevo.webp");
    }

    @Test
    void conTransaccionSinCommit_borraElNuevoYConservaElAnterior() {
        TransactionSynchronizationManager.initSynchronization();
        borradoArchivosDiferido.registrarBorrados("nuevo.webp", "anterior.webp");
        TransactionSynchronization sincronizacion = unicaSincronizacion();

        sincronizacion.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storageService).eliminarFotoPerfil("nuevo.webp");
        verify(storageService, never()).eliminarFotoPerfil("anterior.webp");
    }

    @Test
    void borrar_falloDeAlmacenamiento_noSePropaga() {
        doThrow(new ErrorAlmacenamientoException("Ruta bloqueada", new IOException("Acceso denegado")))
                .when(storageService).eliminarFotoPerfil("bloqueado.webp");

        assertDoesNotThrow(() -> borradoArchivosDiferido.borrar("bloqueado.webp", "prueba"));
    }

    private static TransactionSynchronization unicaSincronizacion() {
        List<TransactionSynchronization> sincronizaciones = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, sincronizaciones.size());
        assertTrue(sincronizaciones.get(0) != null);
        return sincronizaciones.get(0);
    }
}
