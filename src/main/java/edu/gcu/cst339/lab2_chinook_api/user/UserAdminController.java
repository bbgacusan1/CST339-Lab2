package edu.gcu.cst339.lab2_chinook_api.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/users")
class UserAdminController {

    private final UserService userService;

    UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin-users";
    }

    @GetMapping("/{id}/edit")
    String showEditForm(@PathVariable Integer id, Model model) {
        UserSummaryDto user = userService.findById(id);
        UserUpdateDto update = new UserUpdateDto();
        update.setRole(user.role());
        update.setEnabled(user.enabled());
        update.setPasswordChangeRequired(user.passwordChangeRequired());
        model.addAttribute("user", user);
        model.addAttribute("update", update);
        return "admin-user-edit";
    }

    @PostMapping("/{id}")
    String updateUser(@PathVariable Integer id,
            @Valid @ModelAttribute("update") UserUpdateDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.findById(id));
            return "admin-user-edit";
        }
        try {
            userService.updateAccount(id, form);
        } catch (UserValidationException e) {
            bindingResult.reject("user.invalid", e.getMessage());
            model.addAttribute("user", userService.findById(id));
            return "admin-user-edit";
        }
        redirectAttributes.addFlashAttribute("successMessage", "User updated.");
        return "redirect:/admin/users";
    }
}
