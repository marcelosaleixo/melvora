package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.exception.ModuloNaoContratadoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(annotations = Controller.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebExceptionHandler {
    @ExceptionHandler(ModuloNaoContratadoException.class)
    public String moduloNaoContratado(ModuloNaoContratadoException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("erro", ex.getMessage());
        return "redirect:/dashboard";
    }
}
