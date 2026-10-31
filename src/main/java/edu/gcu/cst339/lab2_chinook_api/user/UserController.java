package edu.gcu.cst339.lab2_chinook_api.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    String showRegistrationForm(Model model) {
        model.addAttribute("register", new RegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    String register(@Valid @ModelAttribute("register") RegistrationDto form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form);
        } catch (UserValidationException e) {
            bindingResult.reject("user.invalid", e.getMessage());
            return "register";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful. Please log in.");
        return "redirect:/login";
    }
}
