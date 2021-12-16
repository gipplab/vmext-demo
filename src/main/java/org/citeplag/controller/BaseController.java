package org.citeplag.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Andre Greiner-Petter
 */
@Controller
public class BaseController {
    @RequestMapping("/")
    public String home() {
        return "redirect:/swagger-ui.html";
    }
}
