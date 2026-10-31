package edu.gcu.cst339.lab2_chinook_api.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

/**
 * Serves the Thymeleaf landing page for the application
 * HomeController
 */
@Controller
class HomeController {

    /**
     * Displays the landing page
     * @param model holds the values displayed by the template
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("home", "CST-339 Lab");
        return "home";
    }
}
