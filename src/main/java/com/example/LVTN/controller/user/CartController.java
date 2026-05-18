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
    private CartService cartService; // Gọi đến Interface, Spring Boot tự động kích hoạt CartServiceImpl



    @GetMapping
    public String viewCart(Model model, HttpServletRequest request) {
        // Lưu URI hiện tại để xử lý active menu hoặc quay lại trang nếu cần
        model.addAttribute("currentUri", request.getRequestURI());

        // Lấy giỏ hàng của User từ Database
        Cart cart = cartService.getCartForUser();
        if (cart != null) {
            // Đẩy danh sách các sản phẩm trong giỏ ra giao diện HTML
            model.addAttribute("cartItems", cart.getItems());
        }

        // Đẩy tổng tiền (đã chuyển sang dạng BigDecimal chuẩn) ra giao diện
        model.addAttribute("totalPrice", cartService.getTotalPrice());
        model.addAttribute("totalQuantity", cartService.getTotalQuantity());

        return "user/cart/cart"; // Trỏ đến file templates/user/cart/cart.html
    }

    // 2. Thêm sản phẩm vào giỏ hàng (Nhận từ nút "Thêm vào giỏ" ở trang chi tiết sản phẩm)
    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam int size,
                            @RequestParam(defaultValue = "1") int quantity) {

        cartService.add(productId, size, quantity);

        return "redirect:/cart"; // Thêm xong thì tự chuyển hướng về trang xem giỏ hàng
    }

    // 3. Cập nhật số lượng của một sản phẩm trong giỏ (Khi nhấn nút + hoặc -)
    @PostMapping("/update")
    public String updateCart(@RequestParam Long productId,
                             @RequestParam int size,
                             @RequestParam int quantity) {

        cartService.update(productId, size, quantity);

        return "redirect:/cart"; // Cập nhật xong load lại trang giỏ hàng để cập nhật số tiền mới
    }

    // 4. Xóa hoàn toàn một sản phẩm ra khỏi giỏ hàng (Khi nhấn nút Thùng rác)
    @GetMapping("/delete")
    public String deleteFromCart(@RequestParam Long productId,
                                 @RequestParam int size) {

        cartService.remove(productId, size);

        return "redirect:/cart"; // Xóa xong quay lại trang giỏ hàng
    }

}