package library_management.controllers;

import library_management.models.Book;
import library_management.repository.BookRepository;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/add-book")
    public String showAddForm (Model model) {
        model.addAttribute("book", new Book());
        // Lấy toàn bộ sách để hiển thị bảng bên dưới
        model.addAttribute("books", bookRepository.findAll()); 
        return "admin/add-book";
    }
    
    @GetMapping("/delete-book/{id}")
    public String deleteBook(@PathVariable String id) {
        bookRepository.deleteById(id);
        return "redirect:/admin/add-book?deleted";
    }

    @PostMapping("/add-book")
    public String saveBook(@ModelAttribute Book book, 
                           @RequestParam("imageFile") MultipartFile file) {
        try {
            if (!file.isEmpty()) {
                // 1. Định nghĩa đường dẫn lưu file (trong thư mục static của dự án)
                String uploadDir = "src/main/resources/static/uploads/";
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                
                // 2. Tạo thư mục nếu chưa có
                File uploadPath = new File(uploadDir);
                if (!uploadPath.exists()) uploadPath.mkdirs();

                // 3. Lưu file vào thư mục
                file.transferTo(new File(uploadDir + fileName));

                // 4. Lưu đường dẫn vào database
                book.setImageUrl("/uploads/" + fileName);
            } else if (book.getId() != null) { } // Nếu đang edit mà không chọn file mới thì giữ nguyên ảnh cũ
            bookRepository.save(book);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/?success";
    }

    @GetMapping("/edit-book/{id}")
    public String editBook(@PathVariable String id, Model model) {
        Book book = bookRepository.findById(id).orElse(new Book());
        model.addAttribute("book", book); // Đổ dữ liệu cũ vào Form
        model.addAttribute("books", bookRepository.findAll()); // Vẫn hiện danh sách bên dưới
        return "admin/add-book";
    }
}
