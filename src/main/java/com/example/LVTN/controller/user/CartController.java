package com.example.LVTN.controller.user;

import com.example.LVTN.entity.Cart;
import com.example.LVTN.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@ControllerAdvice
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;



    @GetMapping
    public String viewCart(Model model, HttpServletRequest request) {

        model.addAttribute("currentUri", request.getRequestURI());

        // Lấy giỏ hàng của User từ Database
        Cart cart = cartService.getCartForUser();
        if (cart != null) {

            model.addAttribute("cartItems", cart.getItems());
        }


        model.addAttribute("totalPrice", cartService.getTotalPrice());
        model.addAttribute("totalQuantity", cartService.getTotalQuantity());

        return "user/cart/cart";
    }


    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam int size,
                            @RequestParam(defaultValue = "1") int quantity) {

        cartService.add(productId, size, quantity);

        return "redirect:/cart";
    }


    @PostMapping("/update")
    public String updateCart(@RequestParam Long productId,
                             @RequestParam int size,
                             @RequestParam int quantity) {

        cartService.update(productId, size, quantity);

        return "redirect:/cart";
    }


    @GetMapping("/delete")
    public String deleteFromCart(@RequestParam Long productId,
                                 @RequestParam int size) {

        cartService.remove(productId, size);

        return "redirect:/cart";
    }

}