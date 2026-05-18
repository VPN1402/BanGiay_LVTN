package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.Category;
import com.example.LVTN.entity.Product;
import com.example.LVTN.entity.ProductSize;
import com.example.LVTN.entity.User;
import com.example.LVTN.repository.ContactRepository;
import com.example.LVTN.repository.ProductSizeRepository;
import com.example.LVTN.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductSizeRepository productSizeRepository;

    @Autowired
    private ContactRepository contactRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {

        model.addAttribute("totalProducts", productService.findAll().size());

        model.addAttribute("totalCategories", categoryService.findAll().size());

        model.addAttribute("totalUsers", userService.findAll().size());

        model.addAttribute("totalBrands", brandService.findAll().size());

        model.addAttribute("products", productService.findAll());

        model.addAttribute("users", userService.findAll());

        return "admin/dashboard/dashboard";
    }



    @GetMapping("/products")
    public String manageProducts(Model model) {

        model.addAttribute("products", productService.findAll());

        return "admin/product/list";
    }


    @GetMapping("/product/add")
    public String showAddForm(Model model) {

        model.addAttribute("product", new Product());

        model.addAttribute("categories", categoryService.findAll());

        model.addAttribute("brands", brandService.findAll());

        return "admin/product/add";
    }


    @PostMapping("/product/save")
    public String saveProduct(@ModelAttribute("product") Product product) {

        productService.save(product);

        return "redirect:/admin/products";
    }


    @GetMapping("/product/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id,
                               Model model) {

        Product product = productService.findById(id);

        model.addAttribute("product", product);

        model.addAttribute("categories", categoryService.findAll());

        model.addAttribute("brands", brandService.findAll());

        return "admin/product/add";
    }


    @GetMapping("/product/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id) {

        productService.delete(id);

        return "redirect:/admin/products";
    }




    @GetMapping("/categories")
    public String manageCategories(Model model) {

        model.addAttribute("categories", categoryService.findAll());

        return "admin/category/list";
    }

    @GetMapping("/category/add")
    public String addCategory(Model model) {

        model.addAttribute("category", new Category());

        return "admin/category/add";
    }


    @PostMapping("/category/save")
    public String saveCategory(@ModelAttribute("category") Category category) {

        categoryService.save(category);

        return "redirect:/admin/categories";
    }


    @GetMapping("/category/edit/{id}")
    public String editCategory(@PathVariable Long id,
                               Model model) {

        Category category = categoryService.findById(id);

        model.addAttribute("category", category);

        return "admin/category/add";
    }

    @GetMapping("/category/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {

        categoryService.delete(id);

        return "redirect:/admin/categories";
    }




    @GetMapping("/users")
    public String manageUsers(Model model) {

        model.addAttribute("users", userService.findAll());

        return "admin/user/list";
    }


    @GetMapping("/user/add")
    public String addUser(Model model) {

        model.addAttribute("user", new User());
        model.addAttribute("roles", roleService.findAll());

        return "admin/user/add";
    }


    @PostMapping("/user/save")
    public String saveUser(@ModelAttribute("user") User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
        }
        userService.save(user);

        return "redirect:/admin/users";
    }


    @GetMapping("/user/edit/{id}")
    public String editUser(@PathVariable Long id,
                           Model model) {

        User user = userService.findById(id);

        model.addAttribute("user", user);
        model.addAttribute("roles", roleService.findAll());

        return "admin/user/add";
    }


    @GetMapping("/user/delete/{id}")
    public String deleteUser(@PathVariable Long id) {

        userService.delete(id);

        return "redirect:/admin/users";
    }
    @GetMapping("/inventory")
    public String showInventory(Model model) {
        List<Product> products = productService.findAll();


        long sapHetCount = products.stream()
                .filter(p -> p.getTotalQuantity() > 0 && p.getTotalQuantity() <= 10)
                .count();


        long hetHangCount = products.stream()
                .filter(p -> p.getTotalQuantity() == 0)
                .count();


        model.addAttribute("products", products);
        model.addAttribute("sapHetCount", sapHetCount);
        model.addAttribute("hetHangCount", hetHangCount);

        return "admin/inventory/inventory";
    }
    @PostMapping("/inventory/update")
    public String updateInventory(@RequestParam("productSizeIds") List<Long> sizeIds,
                                  @RequestParam("quantities") List<Integer> quantities) {

        if (sizeIds != null && quantities != null) {
            for (int i = 0; i < sizeIds.size(); i++) {
                Long id = sizeIds.get(i);
                Integer qty = quantities.get(i);


                ProductSize ps = productSizeRepository.findById(id).orElse(null);
                if (ps != null) {
                    ps.setQuantity(qty != null ? qty : 0);
                    productSizeRepository.save(ps);
                }
            }
        }
        return "redirect:/admin/inventory";
    }
    // 1. Trang danh sách hiển thị các lời nhắn liên hệ
    @GetMapping("/contacts")
    public String manageContacts(Model model) {
        // Lấy toàn bộ danh sách tin nhắn xếp theo mới nhất lên đầu
        List<com.example.LVTN.entity.Contact> contacts = contactRepository.findAll();
        model.addAttribute("contacts", contacts);
            return "admin/contact/contact-list"; // Đường dẫn file HTML phía admin
        }

    // 2. Chức năng cập nhật trạng thái "Đã đọc/Đã xử lý" tin nhắn
    @GetMapping("/contact/read/{id}")
    public String markAsRead(@PathVariable("id") Long id) {
        com.example.LVTN.entity.Contact contact = contactRepository.findById(id).orElse(null);
        if (contact != null) {
            contact.setStatus(1); // Chuyển sang 1: Đã đọc
            contactRepository.save(contact);
        }
        return "redirect:/admin/contacts";
    }
}