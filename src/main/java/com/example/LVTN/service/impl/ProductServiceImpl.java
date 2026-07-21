package com.example.LVTN.service.impl;

import com.example.LVTN.entity.Product;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.ProductRepository;
import com.example.LVTN.service.ActivityLogService;
import com.example.LVTN.service.ProductService;
import com.example.LVTN.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private SecurityUtils securityUtils; // Inject SecurityUtils dạng Bean chuẩn

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    // GHI LOG KHI THÊM HOẶC SỬA SẢN PHẨM
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product save(Product product) {
        boolean isNew = (product.getId() == null || product.getId() == 0L);

        Product savedProduct = productRepository.save(product);

        // Gọi qua instance securityUtils đã được inject
        User currentUser = securityUtils.getCurrentLoggedInUser();
        Long userId = (currentUser != null) ? currentUser.getId() : null;
        String fullName = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName() : "Hệ thống";
        String roleName = securityUtils.getCurrentRoleName();

        if (isNew) {
            activityLogService.log(
                    userId,
                    fullName,
                    roleName,
                    "THÊM SẢN PHẨM",
                    fullName + " đã thêm sản phẩm mới: '" + savedProduct.getName() + "' (ID: #" + savedProduct.getId() + ")"
            );
        } else {
            activityLogService.log(
                    userId,
                    fullName,
                    roleName,
                    "SỬA SẢN PHẨM",
                    fullName + " đã cập nhật thông tin sản phẩm: '" + savedProduct.getName() + "' (ID: #" + savedProduct.getId() + ")"
            );
        }

        return savedProduct;
    }

    // GHI LOG KHI XÓA SẢN PHẨM
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Product product = productRepository.findById(id).orElse(null);

        if (product != null) {
            String productName = product.getName();

            productRepository.delete(product);

            // Gọi qua instance securityUtils đã được inject
            User currentUser = securityUtils.getCurrentLoggedInUser();
            Long userId = (currentUser != null) ? currentUser.getId() : null;
            String fullName = (currentUser != null && currentUser.getFullName() != null) ? currentUser.getFullName() : "Hệ thống";
            String roleName = securityUtils.getCurrentRoleName();

            activityLogService.log(
                    userId,
                    fullName,
                    roleName,
                    "XÓA SẢN PHẨM",
                    fullName + " đã xóa vĩnh viễn sản phẩm: '" + productName + "' (ID: #" + id + ")"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> filterProducts(Long categoryId, Long brandId, Double minPrice, Double maxPrice, String sizeName, String keyword, Pageable pageable) {
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String searchSize = (sizeName != null && !sizeName.trim().isEmpty()) ? sizeName.trim() : null;

        return productRepository.filterProducts(categoryId, brandId, minPrice, maxPrice, searchSize, searchKeyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findFeatured() {
        return productRepository.findByFeaturedTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
}