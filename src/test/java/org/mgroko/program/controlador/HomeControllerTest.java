package org.mgroko.program.controlador;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerTest {

    @Test
    void homeShouldReturnHomeViewAndPopulateMessage() {
        HomeController controller = new HomeController();
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.home(model);

        assertEquals("home", view);
        assertEquals("Proyecto levantado correctamente", model.getAttribute("mensaje"));
    }
}
