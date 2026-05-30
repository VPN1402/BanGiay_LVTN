package com.example.LVTN.service.impl;

import com.example.LVTN.entity.Cart;
import com.example.LVTN.entity.CartItem;
import com.example.LVTN.entity.Product;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.CartItemRepository;
import com.example.LVTN.repository.CartRepository;
import com.example.LVTN.repository.ProductRepository;
import com.example.LVTN.repository.UserRepository;
import com.example.LVTN.service.CartService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private HttpSession session; // Tiêm Session để quản lý khách vãng lai

    private static final String SESSION_CART_KEY = "SESSION_CART";

    @Override
    public User getLoggedInUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (email == null || email.equals("anonymousUser")) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public Cart getCartForUser() {
        User user = getLoggedInUser();

        // TRƯỜNG HỢP 1: NẾU ĐÃ ĐĂNG NHẬP -> LẤY TỪ DATABASE
        if (user != null) {
            return cartRepository.findByUserId(user.getId())
                    .orElseGet(() -> {
                        Cart newCart = new Cart();
                        newCart.setUser(user);
                        return cartRepository.save(newCart);
                    });
        }

        // TRƯỜNG HỢP 2: CHƯA ĐĂNG NHẬP -> LẤY TỪ SESSION
        Cart sessionCart = (Cart) session.getAttribute(SESSION_CART_KEY);
        if (sessionCart == null) {
            sessionCart = new Cart(); // Tạo một đối tượng Cart tạm thời trong bộ nhớ
            session.setAttribute(SESSION_CART_KEY, sessionCart);
        }
        return sessionCart;
    }

    @Override
    public void add(Long productId, int size, int quantity) {
        User user = getLoggedInUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        if (user != null) {
            // 1. Nếu đã đăng nhập: Lưu vào DB giống như cũ
            Cart cart = getCartForUser();
            CartItem cartItem = cartItemRepository.findByCartIdAndProductIdAndSize(cart.getId(), productId, size)
                    .orElse(null);

            if (cartItem != null) {
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
            } else {
                cartItem = new CartItem();
                cartItem.setCart(cart);
                cartItem.setProduct(product);
                cartItem.setSize(size);
                cartItem.setQuantity(quantity);
                cartItem.setPrice(product.getPrice());
            }
            cartItemRepository.save(cartItem);
        } else {
            // 2. Nếu chưa đăng nhập: Lưu vào List trong đối tượng Cart của Session
            Cart cart = getCartForUser();

            // Tìm xem trong List items của Session đã có sản phẩm này cùng size chưa
            CartItem existingItem = cart.getItems().stream()
                    .filter(item -> item.getProduct().getId().equals(productId) && item.getSize() == size)
                    .findFirst().orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProduct(product);
                newItem.setSize(size);
                newItem.setQuantity(quantity);
                newItem.setPrice(product.getPrice());
                cart.getItems().add(newItem); // Thêm vào danh sách tạm
            }
            session.setAttribute(SESSION_CART_KEY, cart); // Cập nhật lại Session
        }
    }

    @Override
    public void update(Long productId, int size, int quantity) {
        User user = getLoggedInUser();
        Cart cart = getCartForUser();
        if (cart == null) return;

        if (user != null) {
            // Đã đăng nhập: Update dưới DB
            CartItem cartItem = cartItemRepository.findByCartIdAndProductIdAndSize(cart.getId(), productId, size)
                    .orElse(null);
            if (cartItem != null) {
                if (quantity <= 0) cartItemRepository.delete(cartItem);
                else {
                    cartItem.setQuantity(quantity);
                    cartItemRepository.save(cartItem);
                }
            }
        } else {
            // Chưa đăng nhập: Update trên Session List
            cart.getItems().removeIf(item -> {
                if (item.getProduct().getId().equals(productId) && item.getSize() == size) {
                    if (quantity <= 0) return true; // Xóa nếu số lượng nhỏ hơn hoặc bằng 0
                    item.setQuantity(quantity);
                }
                return false;
            });
            session.setAttribute(SESSION_CART_KEY, cart);
        }
    }

    @Override
    public void remove(Long productId, int size) {
        User user = getLoggedInUser();
        Cart cart = getCartForUser();
        if (cart == null) return;

        if (user != null) {
            cart.getItems().removeIf(item ->
                    item.getProduct().getId().equals(productId)
                            && item.getSize() == size
            );
            cartRepository.save(cart);
        } else {
            // Chưa đăng nhập: Xóa trên Session List
            cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId) && item.getSize() == size);
            session.setAttribute(SESSION_CART_KEY, cart);
        }
    }

    @Override
    public BigDecimal getTotalPrice() {
        Cart cart = getCartForUser();
        if (cart == null || cart.getItems() == null) return BigDecimal.ZERO;

        return cart.getItems().stream()
                .map(CartItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    @Override
    public int getTotalQuantity() {
        Cart cart = getCartForUser();
        if (cart == null || cart.getItems() == null) return 0;


        return cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
    @Override
    @Transactional
    public void clearCart(Long userId) {
        // Dùng ifPresent để xóa an toàn, nếu giỏ hàng trống thì không làm gì cả
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartItemRepository.deleteAll(cart.getItems());
            cartRepository.save(cart); // Lưu lại trạng thái giỏ hàng đã trống
        });
    }
}