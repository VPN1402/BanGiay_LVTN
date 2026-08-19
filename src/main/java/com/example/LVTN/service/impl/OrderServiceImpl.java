package com.example.LVTN.service.impl;

import com.example.LVTN.dto.CheckoutRequest;
import com.example.LVTN.entity.*;
import com.example.LVTN.enums.OrderStatus;
import com.example.LVTN.enums.PaymentStatus;
import com.example.LVTN.repository.*;
import com.example.LVTN.service.ActivityLogService;
import com.example.LVTN.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductSizeRepository productSizeRepository;
    @Autowired private ActivityLogService activityLogService;

    @Override
    public List<Order> findAll() {
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

        // Khởi tạo đơn hàng
        Order order = new Order();
        order.setUser(currentUser);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setOrderStatus(OrderStatus.PENDING.name()); // Mặc định là PENDING chờ xử lý kho
        order.setPaymentStatus(PaymentStatus.UNPAID.name());

        // ===== NGHIỆP VỤ: TỰ ĐỘNG PHÂN LOẠI ĐỘ ƯU TIÊN KHI TẠO ĐƠN =====
        if ("VNPAY".equals(request.getPaymentMethod())) {
            order.setPriority(1); // Chuyển khoản -> Ưu tiên 1 (Lên đầu hàng đợi)
        } else {
            order.setPriority(2); // COD -> Ưu tiên 2 (Xử lý sau)
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            // Kiểm tra xem sản phẩm kích thước đó có tồn tại không
            ProductSize ps = productSizeRepository.findByProductIdAndSize(
                    cartItem.getProduct().getId(),
                    String.valueOf(cartItem.getSize())
            ).orElseThrow(() -> new RuntimeException("Không tìm thấy Size của sản phẩm!"));

            // Chỉ cảnh báo nếu tại thời điểm đặt hàng đã hết sạch sành sanh
            if (ps.getQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm [" + cartItem.getProduct().getName() + "] size "
                        + cartItem.getSize() + " không đủ số lượng. Kho chỉ còn: " + ps.getQuantity());
            }

            // CHÚ Ý: ĐÃ XÓA ĐOẠN TRỪ KHO TẠI ĐÂY THEO NGHIỆP VỤ CHỜ SUBMIT KHO MỚI TRỪ
            // Chuyển CartItem thành OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setSize(String.valueOf(cartItem.getSize()));
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());

            orderItems.add(orderItem);

            BigDecimal lineTotal = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        order.setTotalAmount(totalAmount);

        // Tính toán số tiền cuối cùng (Cộng thêm 30k ship cố định)
        BigDecimal shippingFee = new BigDecimal("30000");
        order.setFinalTotal(totalAmount.add(shippingFee));

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(order);

        // Xóa các sản phẩm khỏi giỏ hàng sau khi tạo đơn thành công
        cartItemRepository.deleteAll(cart.getItems());

        return savedOrder;
    }


    // ===== NGHIỆP VỤ MỚI: LẤY DANH SÁCH ĐƠN HÀNG XẾP THEO ĐỘ ƯU TIÊN VÀ THỜI GIAN =====
    @Override
    public List<Order> getOrdersSortedByPriority() {
        // Lấy các đơn hàng đang chờ xử lý (PENDING) và sắp xếp: Ưu tiên 1 (VNPAY) lên trước, Ưu tiên 2 (COD) sau, đơn cũ làm trước
        return orderRepository.findByOrderStatusOrderByPriorityAscCreatedAtAsc("PENDING");
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

        order.setPaymentStatus(status);

        if ("PAID".equals(status)) {
            order.setOrderStatus(OrderStatus.PENDING.name());
        } else if ("FAILED".equals(status)) {
            order.setOrderStatus(OrderStatus.CANCELLED.name());
        }

        orderRepository.save(order);
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void updateOrderDetails(Long orderId, String orderStatus, String paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId));

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

        // VÌ KHI ĐẶT HÀNG CHƯA TRỪ KHO, NÊN KHI KHÁCH HỦY ĐƠN Ở TRẠNG THÁI PENDING THÌ KHÔNG CẦN CỘNG LẠI KHO NỮA
        order.setOrderStatus("CANCELLED");

        if ("PAID".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("REFUND_PENDING");
        }
        orderRepository.save(order);
    }

    @Override
    public List<Order> findByUser(User user) {
        return orderRepository.findByUser(user);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceivedByQR(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng tương ứng với mã QR này!"));

        // Chỉ cho phép hoàn thành khi đơn hàng đang ở trạng thái SHIPPING
        if (!"SHIPPING".equals(order.getOrderStatus())) {
            throw new RuntimeException("Đơn hàng không ở trạng thái đang giao, không thể xác nhận!");
        }

        // 1. Cập nhật trạng thái đơn hàng thành hoàn thành
        order.setOrderStatus(OrderStatus.DELIVERED.name());

        // 2. Nghiệp vụ: Nếu là đơn COD (thanh toán tiền mặt), khi nhận hàng thành công thì tự chuyển trạng thái tiền thành PAID
        if ("COD".equals(order.getPaymentMethod())) {
            order.setPaymentStatus(PaymentStatus.PAID.name());
        }

        orderRepository.save(order);
    }

    // ===== VÍ DỤ 1: Ghi log khi Nhân viên bấm Xuất Kho =====
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitXuatKho(Long orderId, String usernameNhanVien) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // 1. Kiểm tra tồn kho thực tế
        for (OrderItem item : order.getOrderItems()) {
            ProductSize ps = productSizeRepository.findByProductIdAndSize(
                    item.getProduct().getId(), item.getSize()
            ).orElse(null);

            if (ps == null || ps.getQuantity() < item.getQuantity()) {
                order.setOrderStatus("OUT_OF_STOCK");
                orderRepository.save(order);

                // GHI LOG: Báo sự cố hết hàng
                activityLogService.log(
                        null,
                        usernameNhanVien,
                        "NHÂN VIÊN KHO",
                        "XUẤT KHO THẤT BẠI",
                        "Đơn hàng #" + orderId + " bị hủy xuất kho do thiếu sản phẩm size " + item.getSize()
                );

                return "FAILED_OUT_OF_STOCK";
            }
        }

        // 2. Trừ kho thực tế
        for (OrderItem item : order.getOrderItems()) {
            ProductSize ps = productSizeRepository.findByProductIdAndSize(
                    item.getProduct().getId(), item.getSize()
            ).orElseThrow(() -> new RuntimeException("Lỗi hệ thống khi đối chiếu kho"));

            ps.setQuantity(ps.getQuantity() - item.getQuantity());
            productSizeRepository.save(ps);
        }

        // 3. Đổi trạng thái đơn sang SHIPPING
        order.setOrderStatus("SHIPPING");
        order.setProcessedBy(usernameNhanVien);
        order.setProcessedAt(LocalDateTime.now());
        orderRepository.save(order);

        // GHI LOG THÀNH CÔNG CHO CEO THẤY[cite: 6, 8]
        activityLogService.log(
                null,
                usernameNhanVien,
                "NHÂN VIÊN KHO",
                "DUYỆT XUẤT KHO",
                "Đã xuất kho thành công cho đơn hàng #" + orderId
        );

        return "SUCCESS";
    }
}