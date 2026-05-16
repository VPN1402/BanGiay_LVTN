package com.example.LVTN.controller.admin;

import com.example.LVTN.entity.Category;
import com.example.LVTN.entity.Product;
import com.example.LVTN.entity.User;
import com.example.LVTN.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // DELETE
    @GetMapping("/category/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {

        categoryService.delete(id);

        return "redirect:/admin/categories";
    }



    // LIST USER
    @GetMapping("/users")
    public String manageUsers(Model model) {

        model.addAttribute("users", userService.findAll());

        return "admin/user/list";
    }

    // FORM ADD USER
    @GetMapping("/user/add")
    public String addUser(Model model) {

        model.addAttribute("user", new User());
        model.addAttribute("roles", roleService.findAll());

        return "admin/user/add";
    }


    @PostMapping("/user/save")
    public String saveUser(@ModelAttribute("user") User user) {

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
}