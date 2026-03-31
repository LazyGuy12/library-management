package library_management.controllers;

import library_management.models.Book;
import library_management.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/add-book")
    public String showAddForm (Model model) {
        model.addAttribute("book", new Book());
        return "admin/add-book";
    }

    @PostMapping("/add-book")
    public String saveBook (@ModelAttribute Book book) {
        //co the them logic kiem tra trung isbn o day
        bookRepository.save(book);
        return "redirect:/books";
    }
}
