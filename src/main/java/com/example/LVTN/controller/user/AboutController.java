package com.example.LVTN.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AboutController {
    @GetMapping("/about")
    public String about(Model model, HttpServletRequest request){
        model.addAttribute("currentUri", request.getRequestURI());
        return "user/about/about";
    }

}
