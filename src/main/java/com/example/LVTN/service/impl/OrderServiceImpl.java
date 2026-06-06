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
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductSizeRepository productSizeRepository;

    @Override
    public  List<Order> findAll(){
        return orderRepository.findAll();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order placeOrder(User currentUser, CheckoutRequest request) {


        Cart cart = cartRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại!"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống, không thể đặt hàng!");
        }

       // khởi tạo đơn hàng
        Order order = new Order();
        order.setUser(currentUser);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setOrderStatus(OrderStatus.PENDING.name());

        //mặc định là UNPAID
        order.setPaymentStatus(PaymentStatus.UNPAID.name());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();


        for (CartItem cartItem : cart.getItems()) {
            // ktra tồn
            ProductSize ps = productSizeRepository.findByProductIdAndSize(
                    cartItem.getProduct().getId(),
                    String.valueOf(cartItem.getSize())
            ).orElseThrow(() -> new RuntimeException("Không tìm thấy Size của sản phẩm!"));

            if (ps.getQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm [" + cartItem.getProduct().getName() + "] size "
                        + cartItem.getSize() + " không đủ số lượng. Kho chỉ còn: " + ps.getQuantity());
            }

            // trừ kho
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

            // tongtien = gia * sl
            BigDecimal lineTotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }


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



    @Override
    public Order findById(Long id) {

        return orderRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void updatePaymentStatus(Long orderId, String status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

        // Gán trạng thái chuỗi "PAID" hoặc "FAILED" được trả về từ luồng VNPAY/MOMO
        order.setPaymentStatus(status);

        // Nếu thanh toán thành công (PAID), bạn có thể đổi trạng thái xử lý đơn hàng nếu muốn
        if ("PAID".equals(status)) {

            order.setOrderStatus(OrderStatus.PENDING.name());
        } else if ("FAILED".equals(status)) {
            order.setOrderStatus(OrderStatus.CANCELLED.name()); // Thanh toán lỗi thì tự động hủy đơn
        }

        orderRepository.save(order);
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


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));


        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new RuntimeException("Đơn hàng này không thể hủy!");
        }


        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này!");
        }


        for (OrderItem item : order.getOrderItems()) {
            ProductSize ps = productSizeRepository.findByProductIdAndSize(
                            item.getProduct().getId(), item.getSize())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không còn trong kho"));

            ps.setQuantity(ps.getQuantity() + item.getQuantity());
            productSizeRepository.save(ps);
        }

        order.setOrderStatus("CANCELLED");

        if ("PAID".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("REFUND_PENDING");
        }
        orderRepository.save(order);
    }


}