package library_management.controllers;

import library_management.models.Book;
import library_management.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/add-book")
    public String showAddForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("books", bookRepository.findAll());
        return "admin/add-book";
    }

    @PostMapping("/add-book")
    public String saveBook(@ModelAttribute Book book, @RequestParam("imageFile") MultipartFile file) {
        try {
            if (!file.isEmpty()) {
                // Xử lý upload ảnh mới
                String uploadDir = "src/main/resources/static/uploads/";
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                File uploadPath = new File(uploadDir);
                if (!uploadPath.exists()) uploadPath.mkdirs();
                file.transferTo(new File(uploadDir + fileName));
                book.setImageUrl("/uploads/" + fileName);
            } else if (book.getId() != null) {
                // BUG FIX: Nếu là UPDATE và không chọn ảnh mới, lấy lại imageUrl cũ từ DB
                bookRepository.findById(book.getId()).ifPresent(oldBook -> {
                    book.setImageUrl(oldBook.getImageUrl());
                });
            }

            // Lưu vào DB: Nếu book có ID, MongoDB tự động UPDATE. Nếu không có ID, nó sẽ INSERT.
            bookRepository.save(book);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/admin/add-book?success";
    }

    @GetMapping("/edit-book/{id}")
    public String editBook(@PathVariable String id, Model model) {
        Optional<Book> book = bookRepository.findById(id);
        if (book.isPresent()) {
            model.addAttribute("book", book.get());
        } else {
            model.addAttribute("book", new Book());
        }
        model.addAttribute("books", bookRepository.findAll());
        return "admin/add-book";
    }

    @GetMapping("/delete-book/{id}")
    public String deleteBook(@PathVariable String id) {
        bookRepository.deleteById(id);
        return "redirect:/admin/add-book?deleted";
    }
}