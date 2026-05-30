package com.example.LVTN.service.impl;

import com.example.LVTN.dto.CheckoutRequest;
import com.example.LVTN.entity.*;
import com.example.LVTN.enums.OrderStatus;
import com.example.LVTN.enums.PaymentStatus;
import com.example.LVTN.repository.*;
import com.example.LVTN.service.OrderService; // 1. THÊM IMPORT INTERFACE CỦA BẠN
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService { // 2. THÊM "implements OrderService" Ở ĐÂY

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductSizeRepository productSizeRepository;

    @Override
    public  List<Order> findAll(){
        return orderRepository.findAll();
    }

    // BẮT BUỘC CÓ @Transactional để chống lỗi hụt dữ liệu
    @Override // 3. THÊM @Override CHO HÀM ĐẶT HÀNG
    @Transactional(rollbackFor = Exception.class)
    public Order placeOrder(User currentUser, CheckoutRequest request) {

        // 1. Lấy giỏ hàng của user
        Cart cart = cartRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại!"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

        // 2. Khởi tạo đơn hàng (Order)
        Order order = new Order();
        order.setUser(currentUser);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setOrderStatus(OrderStatus.PENDING.name());

        // Trạng thái thanh toán mặc định là UNPAID (Chưa thanh toán)
        order.setPaymentStatus(PaymentStatus.UNPAID.name());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 3. Xử lý từng sản phẩm trong giỏ hàng
        for (CartItem cartItem : cart.getItems()) {
            // -- NGHIỆP VỤ: KIỂM TRA TỒN KHO THỰC TẾ --
            ProductSize ps = productSizeRepository.findByProductIdAndSize(
                    cartItem.getProduct().getId(),
                    String.valueOf(cartItem.getSize())
            ).orElseThrow(() -> new RuntimeException("Không tìm thấy Size của sản phẩm!"));

            if (ps.getQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm [" + cartItem.getProduct().getName() + "] size "
                        + cartItem.getSize() + " không đủ số lượng. Kho chỉ còn: " + ps.getQuantity());
            }

            // -- NGHIỆP VỤ: TRỪ KHO --
            ps.setQuantity(ps.getQuantity() - cartItem.getQuantity());
            productSizeRepository.save(ps);

            // Chuyển CartItem thành OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setSize(String.valueOf(cartItem.getSize()));
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());

            orderItems.add(orderItem);

            // Tính tổng tiền = Giá * Số lượng
            BigDecimal lineTotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        // 4. Lưu tổng tiền đơn hàng (Tạm tính)
        order.setTotalAmount(totalAmount);

        // ĐỒNG BỘ GIAO DIỆN: Tính toán số tiền cuối cùng (Cộng thêm 30k ship cố định từ giao diện HTML)
        BigDecimal shippingFee = new BigDecimal("30000");
        order.setFinalTotal(totalAmount.add(shippingFee)); // Thiết lập số tiền này để VNPAY lấy đi thanh toán

        order.setOrderItems(orderItems); // CascadeType.ALL sẽ tự lưu các orderItems
        Order savedOrder = orderRepository.save(order);

        // 5. Xóa các sản phẩm khỏi giỏ hàng
        cartItemRepository.deleteAll(cart.getItems());

        return savedOrder;
    }

    // ==================== 4. BỔ SUNG 2 HÀM CÒN THIẾU DƯỚI ĐÂY ====================

    @Override
    public Order findById(Long id) {
        // Tìm đơn hàng theo ID phục vụ cho Controller kiểm tra thông tin, nếu không thấy trả về null
        return orderRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void updatePaymentStatus(Long orderId, String status) {
        // Tìm kiếm đơn hàng cần cập nhật trạng thái
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        // Gán trạng thái chuỗi "PAID" hoặc "FAILED" được trả về từ luồng VNPAY/MOMO
        order.setPaymentStatus(status);

        // Nếu thanh toán thành công (PAID), bạn có thể đổi trạng thái xử lý đơn hàng nếu muốn
        if ("PAID".equals(status)) {
            // Ví dụ: Đơn đã thanh toán thành công thì giữ nguyên chờ duyệt hoặc chuyển trạng thái khác
            order.setOrderStatus(OrderStatus.PENDING.name());
        } else if ("FAILED".equals(status)) {
            order.setOrderStatus(OrderStatus.CANCELLED.name()); // Thanh toán lỗi thì tự động hủy đơn
        }

        orderRepository.save(order); // Lưu cập nhật vào Cơ sở dữ liệu
    }
    // ============================================================================
    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    @Override
    @Transactional
    public void updateOrderDetails(Long orderId, String orderStatus, String paymentStatus) {
        // 1. Tìm đơn hàng trong DB
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        // 2. Cập nhật giá trị mới
        order.setOrderStatus(orderStatus);
        order.setPaymentStatus(paymentStatus);

        // 3. Lưu lại vào DB
        orderRepository.save(order);
    }
    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        order.setOrderStatus(newStatus);
        orderRepository.save(order);
    }
}